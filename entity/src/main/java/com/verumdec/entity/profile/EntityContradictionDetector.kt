package com.verumdec.entity.profile

import com.verumdec.core.model.Contradiction
import com.verumdec.core.model.ContradictionType
import com.verumdec.core.model.LegalTrigger
import com.verumdec.core.model.Statement
import java.util.UUID
import kotlin.math.abs

/**
 * EntityContradictionDetector detects contradictions at the entity level.
 *
 * Logic:
 * - Build an EntityProfile for every person, company, site, and legal structure
 * - Track attributes: name, role, actions, claims, dates, financial amounts
 * - Compare EntityProfiles across documents
 * - Flag contradictions when:
 *   - A person denies something they previously admitted
 *   - Document A says X; Document B says NOT X
 *   - A financial figure changes without explanation
 */
class EntityContradictionDetector {
    
    private val profiles = mutableMapOf<String, EntityProfile>()
    
    /**
     * Check if entity profiles exist.
     */
    fun hasProfiles(): Boolean = profiles.isNotEmpty()
    
    /**
     * Get all entity profiles.
     */
    fun getAllProfiles(): List<EntityProfile> = profiles.values.toList()
    
    /**
     * Get a profile by entity ID.
     */
    fun getProfile(entityId: String): EntityProfile? = profiles[entityId]
    
    /**
     * Get a profile by name (case-insensitive).
     */
    fun getProfileByName(name: String): EntityProfile? {
        return profiles.values.find { it.isKnownAs(name) }
    }
    
    /**
     * Add or update an entity profile.
     */
    fun addProfile(profile: EntityProfile) {
        profiles[profile.id] = profile
    }
    
    /**
     * Build entity profiles from statements.
     *
     * @param statements List of statements to process
     */
    fun buildProfilesFromStatements(statements: List<Statement>) {
        // Group statements by speaker
        val statementsBySpeaker = statements.groupBy { it.speaker }
        
        for ((speaker, speakerStatements) in statementsBySpeaker) {
            val existingProfile = getProfileByName(speaker)
            val profile = existingProfile ?: EntityProfile(name = speaker)
            
            speakerStatements.forEach { statement ->
                profile.statementIds.add(statement.id)
                profile.documentIds.add(statement.documentId)
                statement.timestamp?.let { profile.timelineFootprint.add(it) }
                
                // Extract claims from statements
                val claim = EntityClaim(
                    text = statement.text,
                    timestamp = statement.timestamp,
                    documentId = statement.documentId,
                    statementId = statement.id,
                    category = mapLegalCategoryToClaimCategory(statement.legalCategory)
                )
                profile.addClaim(claim)
                
                // Update behavioral profile
                statement.timestamp?.let { ts ->
                    profile.behavioralProfile.sentimentTrend.add(
                        SentimentDataPoint(ts, statement.sentiment, statement.id)
                    )
                    profile.behavioralProfile.certaintyTrend.add(
                        CertaintyDataPoint(ts, statement.certainty, statement.id)
                    )
                }
                
                // Detect financial figures in statement
                extractFinancialFigures(statement, profile)
            }
            
            profiles[profile.id] = profile
        }
    }
    
    /**
     * Detect entity-level contradictions.
     *
     * @return List of detected contradictions
     */
    fun detectEntityContradictions(): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Check each entity for internal contradictions
        for (profile in profiles.values) {
            contradictions.addAll(detectInternalContradictions(profile))
            contradictions.addAll(detectFinancialContradictions(profile))
        }
        
        // Check for cross-entity contradictions
        val profileList = profiles.values.toList()
        for (i in profileList.indices) {
            for (j in i + 1 until profileList.size) {
                contradictions.addAll(
                    detectCrossEntityContradictions(profileList[i], profileList[j])
                )
            }
        }
        
        return contradictions
    }

    /**
     * Detect entity-level contradictions using provided profiles.
     *
     * @param externalProfiles Map of entity IDs to EntityProfiles
     * @return List of detected contradictions
     */
    fun detectEntityContradictions(externalProfiles: Map<String, EntityProfile>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Check each entity for internal contradictions
        for (profile in externalProfiles.values) {
            contradictions.addAll(detectInternalContradictions(profile))
            contradictions.addAll(detectFinancialContradictions(profile))
        }
        
        // Check for cross-entity contradictions
        val profileList = externalProfiles.values.toList()
        for (i in profileList.indices) {
            for (j in i + 1 until profileList.size) {
                contradictions.addAll(
                    detectCrossEntityContradictions(profileList[i], profileList[j])
                )
            }
        }
        
        return contradictions
    }
    
    /**
     * Detect internal contradictions within an entity's claims.
     * (e.g., A person denies something they previously admitted)
     */
    private fun detectInternalContradictions(profile: EntityProfile): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        val admissions = profile.claims.filter { it.category == ClaimCategory.ADMISSION }
        val denials = profile.claims.filter { it.category == ClaimCategory.DENIAL }
        
        // Check if any denial contradicts a prior admission
        for (admission in admissions) {
            for (denial in denials) {
                if (statementsContradict(admission.text, denial.text)) {
                    val severity = calculateContradictionSeverity(admission, denial)
                    contradictions.add(
                        Contradiction(
                            type = ContradictionType.ENTITY,
                            sourceStatement = createStatementFromClaim(admission, profile),
                            targetStatement = createStatementFromClaim(denial, profile),
                            sourceDocument = admission.documentId,
                            sourceLineNumber = 0,
                            severity = severity,
                            description = "${profile.name} denied something they previously admitted: " +
                                "'${admission.text.take(50)}...' vs '${denial.text.take(50)}...'",
                            legalTrigger = LegalTrigger.UNRELIABLE_TESTIMONY,
                            affectedEntities = listOf(profile.id)
                        )
                    )
                }
            }
        }
        
        return contradictions
    }
    
    /**
     * Detect financial contradictions where figures change without explanation.
     */
    private fun detectFinancialContradictions(profile: EntityProfile): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        val figures = profile.financialFigures.sortedBy { it.timestamp ?: 0 }
        
        // Group by similar descriptions/contexts
        val figuresByContext = figures.groupBy { normalizeContext(it.description) }
        
        for ((_, contextFigures) in figuresByContext) {
            if (contextFigures.size >= 2) {
                for (i in 0 until contextFigures.size - 1) {
                    val earlier = contextFigures[i]
                    val later = contextFigures[i + 1]
                    
                    // Check for significant unexplained changes
                    val percentChange = abs(later.amount - earlier.amount) / earlier.amount.coerceAtLeast(1.0) * 100
                    if (percentChange > 10) { // More than 10% change
                        val severity = when {
                            percentChange > 50 -> 9
                            percentChange > 25 -> 7
                            else -> 5
                        }
                        
                        contradictions.add(
                            Contradiction(
                                type = ContradictionType.FINANCIAL,
                                sourceStatement = createStatementFromFinancial(earlier, profile),
                                targetStatement = createStatementFromFinancial(later, profile),
                                sourceDocument = earlier.documentId,
                                sourceLineNumber = 0,
                                severity = severity,
                                description = "Financial figure changed from ${earlier.currency}${earlier.amount} to " +
                                    "${later.currency}${later.amount} (${percentChange.toInt()}% change) without explanation",
                                legalTrigger = LegalTrigger.FINANCIAL_DISCREPANCY,
                                affectedEntities = listOf(profile.id)
                            )
                        )
                    }
                }
            }
        }
        
        return contradictions
    }
    
    /**
     * Detect contradictions between two entities.
     * (e.g., Entity A claims X, Entity B claims NOT X about the same topic)
     */
    private fun detectCrossEntityContradictions(
        profileA: EntityProfile,
        profileB: EntityProfile
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Compare claims from both entities
        for (claimA in profileA.claims) {
            for (claimB in profileB.claims) {
                if (claimsContradict(claimA, claimB)) {
                    val severity = calculateCrossEntitySeverity(claimA, claimB)
                    contradictions.add(
                        Contradiction(
                            type = ContradictionType.CROSS_DOCUMENT,
                            sourceStatement = createStatementFromClaim(claimA, profileA),
                            targetStatement = createStatementFromClaim(claimB, profileB),
                            sourceDocument = claimA.documentId,
                            sourceLineNumber = 0,
                            severity = severity,
                            description = "${profileA.name} claims '${claimA.text.take(40)}...' " +
                                "but ${profileB.name} contradicts with '${claimB.text.take(40)}...'",
                            legalTrigger = LegalTrigger.MISREPRESENTATION,
                            affectedEntities = listOf(profileA.id, profileB.id)
                        )
                    )
                }
            }
        }
        
        return contradictions
    }
    
    /**
     * Check if two statement texts contradict each other.
     */
    private fun statementsContradict(text1: String, text2: String): Boolean {
        val normalized1 = text1.lowercase()
        val normalized2 = text2.lowercase()
        
        // Check for explicit negation patterns
        val negationPatterns = listOf(
            "never" to "always",
            "did not" to "did",
            "didn't" to "did",
            "was not" to "was",
            "wasn't" to "was",
            "no" to "yes",
            "denied" to "admitted",
            "refuse" to "accept",
            "false" to "true"
        )
        
        for ((neg, pos) in negationPatterns) {
            if ((normalized1.contains(neg) && normalized2.contains(pos)) ||
                (normalized1.contains(pos) && normalized2.contains(neg))) {
                // Additional check: ensure they're about similar topics
                val similarity = calculateTextSimilarity(normalized1, normalized2)
                if (similarity > 0.3) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Check if two claims contradict each other.
     */
    private fun claimsContradict(claimA: EntityClaim, claimB: EntityClaim): Boolean {
        // Claims about different subjects can't contradict
        if (claimA.subject.isNotBlank() && claimB.subject.isNotBlank() &&
            !claimA.subject.equals(claimB.subject, ignoreCase = true)) {
            return false
        }
        
        return statementsContradict(claimA.text, claimB.text)
    }
    
    /**
     * Calculate text similarity using word overlap.
     */
    private fun calculateTextSimilarity(text1: String, text2: String): Double {
        val words1 = text1.split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val words2 = text2.split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toDouble() / union.toDouble()
    }
    
    /**
     * Calculate contradiction severity.
     */
    private fun calculateContradictionSeverity(
        admission: EntityClaim,
        denial: EntityClaim
    ): Int {
        var severity = 6 // Base severity for admission/denial contradiction
        
        // Increase severity if both are in legal contexts
        if (admission.category == ClaimCategory.ADMISSION) severity += 2
        
        // Increase if claims are close in time (intentional contradiction)
        val timeDiff = abs((admission.timestamp ?: 0) - (denial.timestamp ?: 0))
        if (timeDiff < 86400000) severity += 1 // Within 24 hours
        
        return severity.coerceIn(1, 10)
    }
    
    /**
     * Calculate severity for cross-entity contradictions.
     */
    private fun calculateCrossEntitySeverity(
        claimA: EntityClaim,
        claimB: EntityClaim
    ): Int {
        var severity = 5 // Base severity
        
        // Higher severity for factual claims
        if (claimA.category == ClaimCategory.FACTUAL || 
            claimB.category == ClaimCategory.FACTUAL) {
            severity += 2
        }
        
        // Higher severity for financial claims
        if (claimA.category == ClaimCategory.FINANCIAL || 
            claimB.category == ClaimCategory.FINANCIAL) {
            severity += 2
        }
        
        return severity.coerceIn(1, 10)
    }
    
    /**
     * Extract financial figures from a statement.
     * Each pattern captures the amount in group 1 and identifies currency from the match.
     */
    private fun extractFinancialFigures(statement: Statement, profile: EntityProfile) {
        // Currency patterns with standardized group structure (amount in group 1)
        data class CurrencyPattern(val regex: Regex, val currency: String)
        
        val patterns = listOf(
            CurrencyPattern(Regex("\\$([0-9,]+\\.?[0-9]*)"), "USD"),
            CurrencyPattern(Regex("£([0-9,]+\\.?[0-9]*)"), "GBP"),
            CurrencyPattern(Regex("€([0-9,]+\\.?[0-9]*)"), "EUR"),
            CurrencyPattern(Regex("([0-9,]+\\.?[0-9]*)\\s*(?:dollars|USD)"), "USD"),
            CurrencyPattern(Regex("([0-9,]+\\.?[0-9]*)\\s*(?:AUD)"), "AUD"),
            CurrencyPattern(Regex("([0-9,]+\\.?[0-9]*)\\s*(?:GBP|pounds)"), "GBP"),
            CurrencyPattern(Regex("([0-9,]+\\.?[0-9]*)\\s*(?:EUR|euros)"), "EUR")
        )
        
        for ((pattern, currency) in patterns) {
            pattern.findAll(statement.text).forEach { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    profile.addFinancialFigure(
                        FinancialFigure(
                            amount = amount,
                            currency = currency,
                            description = statement.text.take(100),
                            timestamp = statement.timestamp,
                            documentId = statement.documentId,
                            context = statement.context
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Normalize context for grouping.
     */
    private fun normalizeContext(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .sorted()
            .take(5)
            .joinToString(" ")
    }
    
    /**
     * Create a Statement from an EntityClaim.
     */
    private fun createStatementFromClaim(
        claim: EntityClaim,
        profile: EntityProfile
    ): Statement {
        return Statement(
            id = claim.statementId,
            speaker = profile.name,
            text = claim.text,
            documentId = claim.documentId,
            documentName = claim.documentId,
            lineNumber = 0,
            timestamp = claim.timestamp,
            legalCategory = mapClaimCategoryToLegalCategory(claim.category)
        )
    }
    
    /**
     * Create a Statement from a FinancialFigure.
     */
    private fun createStatementFromFinancial(
        figure: FinancialFigure,
        profile: EntityProfile
    ): Statement {
        return Statement(
            id = figure.id,
            speaker = profile.name,
            text = "${figure.currency}${figure.amount}: ${figure.description}",
            documentId = figure.documentId,
            documentName = figure.documentId,
            lineNumber = 0,
            timestamp = figure.timestamp,
            legalCategory = com.verumdec.core.model.LegalCategory.FINANCIAL
        )
    }
    
    /**
     * Map LegalCategory to ClaimCategory.
     */
    private fun mapLegalCategoryToClaimCategory(
        category: com.verumdec.core.model.LegalCategory
    ): ClaimCategory {
        return when (category) {
            com.verumdec.core.model.LegalCategory.ADMISSION -> ClaimCategory.ADMISSION
            com.verumdec.core.model.LegalCategory.DENIAL -> ClaimCategory.DENIAL
            com.verumdec.core.model.LegalCategory.PROMISE -> ClaimCategory.PROMISE
            com.verumdec.core.model.LegalCategory.FINANCIAL -> ClaimCategory.FINANCIAL
            com.verumdec.core.model.LegalCategory.ASSERTION -> ClaimCategory.ASSERTION
            else -> ClaimCategory.FACTUAL
        }
    }
    
    /**
     * Map ClaimCategory to LegalCategory.
     */
    private fun mapClaimCategoryToLegalCategory(
        category: ClaimCategory
    ): com.verumdec.core.model.LegalCategory {
        return when (category) {
            ClaimCategory.ADMISSION -> com.verumdec.core.model.LegalCategory.ADMISSION
            ClaimCategory.DENIAL -> com.verumdec.core.model.LegalCategory.DENIAL
            ClaimCategory.PROMISE -> com.verumdec.core.model.LegalCategory.PROMISE
            ClaimCategory.FINANCIAL -> com.verumdec.core.model.LegalCategory.FINANCIAL
            ClaimCategory.ASSERTION -> com.verumdec.core.model.LegalCategory.ASSERTION
            else -> com.verumdec.core.model.LegalCategory.GENERAL
        }
    }
    
    /**
     * Clear all profiles.
     */
    fun clear() {
        profiles.clear()
    }
}
