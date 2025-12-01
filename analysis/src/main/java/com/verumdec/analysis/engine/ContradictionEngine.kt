package com.verumdec.analysis.engine

import com.verumdec.core.model.BehavioralAnomaly
import com.verumdec.core.model.Contradiction
import com.verumdec.core.model.ContradictionReport
import com.verumdec.core.model.ContradictionType
import com.verumdec.core.model.EntityInvolvement
import com.verumdec.core.model.LegalTrigger
import com.verumdec.core.model.LegalTriggerEvidence
import com.verumdec.core.model.Statement
import com.verumdec.core.model.StatementIndex
import com.verumdec.core.model.TimelineConflict
import com.verumdec.core.model.TimelineEvent
import com.verumdec.core.model.VerificationStatus
import com.verumdec.entity.profile.EntityContradictionDetector
import com.verumdec.entity.profile.EntityProfile
import com.verumdec.timeline.detection.TimelineContradictionDetector
import java.util.UUID
import java.util.logging.Logger

/**
 * ContradictionEngine is the core "truth engine" of the Verumdec system.
 *
 * It runs a multi-pass comparison:
 * - Pass 1: Compare every statement against every other statement in the same document
 * - Pass 2: Compare statements across imported documents
 * - Pass 3: Run cross-modal contradiction checks (text vs timeline vs entities vs metadata)
 * - Pass 4: Run linguistic drift detection on each speaker's statements
 *
 * Returns a unified list of contradictions with:
 * - Source document
 * - Line number or chunk index
 * - Contradicting statement pairs
 * - Severity score (1-10)
 * - Recommended legal trigger
 *
 * Self-verification checks:
 * 1. That statements were indexed
 * 2. That embeddings were generated
 * 3. That timeline objects exist
 * 4. That entity profiles exist
 * 5. That at least one contradiction pass returned results
 */
class ContradictionEngine {
    
    companion object {
        /** Threshold in days for considering timestamps as conflicting */
        private const val DAY_DIFFERENCE_THRESHOLD_DAYS = 1L
        /** Milliseconds in a day */
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
    
    private val logger = Logger.getLogger(ContradictionEngine::class.java.name)
    
    // Core components
    private val statementIndex = StatementIndex()
    private val semanticGenerator = SemanticEmbeddingGenerator()
    private val entityDetector = EntityContradictionDetector()
    private val timelineDetector = TimelineContradictionDetector()
    private val linguisticDetector = LinguisticDriftDetector()
    
    // Results from each pass
    private val pass1Results = mutableListOf<Contradiction>()
    private val pass2Results = mutableListOf<Contradiction>()
    private val pass3Results = mutableListOf<Contradiction>()
    private val pass4Results = mutableListOf<Contradiction>()
    
    // All anomalies and conflicts
    private val allAnomalies = mutableListOf<BehavioralAnomaly>()
    private val allTimelineConflicts = mutableListOf<TimelineConflict>()
    
    // Verification tracking
    private val warnings = mutableListOf<String>()
    private val autoCorrections = mutableListOf<String>()
    
    /**
     * Get the statement index.
     */
    fun getStatementIndex(): StatementIndex = statementIndex
    
    /**
     * Get all entity profiles.
     */
    fun getEntityProfiles(): List<EntityProfile> = entityDetector.getAllProfiles()
    
    /**
     * Get entity profiles as a map.
     */
    fun getEntityProfilesMap(): Map<String, EntityProfile> {
        return entityDetector.getAllProfiles().associateBy { it.name }
    }
    
    /**
     * Get all timeline events.
     */
    fun getTimelineEvents(): List<TimelineEvent> = timelineDetector.getAllEvents()
    
    /**
     * Get all behavioral anomalies.
     */
    fun getBehavioralAnomalies(): List<BehavioralAnomaly> = allAnomalies.toList()
    
    /**
     * Index statements from documents.
     *
     * @param statements List of statements to index
     */
    fun indexStatements(statements: List<Statement>) {
        statementIndex.addStatements(statements)
        logger.info("Indexed ${statements.size} statements")
    }
    
    /**
     * Generate semantic embeddings for all indexed statements.
     */
    fun generateEmbeddings() {
        val allStatements = statementIndex.getAllStatements()
        if (allStatements.isEmpty()) {
            warnings.add("No statements to generate embeddings for")
            return
        }
        
        // Build vocabulary from all statement texts
        val texts = allStatements.map { it.text }
        semanticGenerator.buildVocabulary(texts)
        
        // Generate embedding for each statement
        for (statement in allStatements) {
            val embedding = semanticGenerator.generateEmbedding(statement.text)
            statementIndex.updateEmbedding(statement.id, embedding)
        }
        
        logger.info("Generated embeddings for ${allStatements.size} statements")
    }
    
    /**
     * Build entity profiles from indexed statements.
     */
    fun buildEntityProfiles() {
        entityDetector.buildProfilesFromStatements(statementIndex.getAllStatements())
        logger.info("Built ${entityDetector.getAllProfiles().size} entity profiles")
    }
    
    /**
     * Build timeline from indexed statements.
     */
    fun buildTimeline() {
        timelineDetector.buildTimelineFromStatements(statementIndex.getAllStatements())
        logger.info("Built timeline with ${timelineDetector.getAllEvents().size} events")
    }
    
    /**
     * Run the full multi-pass contradiction analysis.
     *
     * @param caseId Identifier for this analysis
     * @return ContradictionReport containing all findings
     */
    fun runFullAnalysis(caseId: String): ContradictionReport {
        // Self-verification and auto-correction
        val verificationStatus = selfVerify()
        
        // Run all passes
        logger.info("Running Pass 1: Intra-document comparison")
        runPass1IntraDocumentComparison()
        
        logger.info("Running Pass 2: Cross-document comparison")
        runPass2CrossDocumentComparison()
        
        logger.info("Running Pass 3: Cross-modal comparison")
        runPass3CrossModalComparison()
        
        logger.info("Running Pass 4: Linguistic drift detection")
        runPass4LinguisticDriftDetection()
        
        // Collect all contradictions
        val allContradictions = (pass1Results + pass2Results + pass3Results + pass4Results)
            .distinctBy { it.id }
        
        // Collect timeline conflicts
        allTimelineConflicts.addAll(timelineDetector.getConflicts())
        
        // Build entity involvement map
        val entityInvolvement = buildEntityInvolvementMap(allContradictions)
        
        // Build document links
        val documentLinks = buildDocumentLinks(allContradictions)
        
        // Build severity breakdown
        val severityBreakdown = allContradictions.groupingBy { it.severity }.eachCount()
        
        // Build legal trigger evidence
        val legalTriggers = buildLegalTriggerEvidence(allContradictions)
        
        // Generate summary
        val summary = generateSummary(allContradictions, allAnomalies, allTimelineConflicts)
        
        logger.info("Analysis complete: ${allContradictions.size} contradictions found")
        
        return ContradictionReport(
            caseId = caseId,
            totalContradictions = allContradictions.size,
            contradictions = allContradictions,
            timelineConflicts = allTimelineConflicts,
            behavioralAnomalies = allAnomalies,
            affectedEntities = entityInvolvement,
            documentLinks = documentLinks,
            severityBreakdown = severityBreakdown,
            legalTriggers = legalTriggers,
            summary = summary,
            verificationStatus = verificationStatus
        )
    }
    
    /**
     * Pass 1: Compare every statement against every other statement in the same document.
     */
    private fun runPass1IntraDocumentComparison() {
        pass1Results.clear()
        
        for (documentId in statementIndex.getDocuments()) {
            val docStatements = statementIndex.getStatementsByDocument(documentId)
            
            for (i in docStatements.indices) {
                for (j in i + 1 until docStatements.size) {
                    val statementA = docStatements[i]
                    val statementB = docStatements[j]
                    
                    val contradiction = checkForContradiction(
                        statementA, statementB,
                        ContradictionType.DIRECT
                    )
                    contradiction?.let { pass1Results.add(it) }
                }
            }
        }
        
        logger.info("Pass 1 found ${pass1Results.size} contradictions")
    }
    
    /**
     * Pass 2: Compare statements across imported documents.
     */
    private fun runPass2CrossDocumentComparison() {
        pass2Results.clear()
        
        val documents = statementIndex.getDocuments().toList()
        
        for (i in documents.indices) {
            for (j in i + 1 until documents.size) {
                val docAStatements = statementIndex.getStatementsByDocument(documents[i])
                val docBStatements = statementIndex.getStatementsByDocument(documents[j])
                
                for (statementA in docAStatements) {
                    for (statementB in docBStatements) {
                        val contradiction = checkForContradiction(
                            statementA, statementB,
                            ContradictionType.CROSS_DOCUMENT
                        )
                        contradiction?.let { pass2Results.add(it) }
                    }
                }
            }
        }
        
        logger.info("Pass 2 found ${pass2Results.size} contradictions")
    }
    
    /**
     * Pass 3: Run cross-modal contradiction checks
     * (text vs timeline vs entities vs metadata).
     */
    private fun runPass3CrossModalComparison() {
        pass3Results.clear()
        
        // Get entity contradictions
        val entityContradictions = entityDetector.detectEntityContradictions()
        pass3Results.addAll(entityContradictions)
        
        // Get timeline contradictions
        val timelineContradictions = timelineDetector.detectTimelineContradictions()
        pass3Results.addAll(timelineContradictions)
        
        // Cross-check timeline events against statement claims
        pass3Results.addAll(crossCheckTimelineWithStatements())
        
        logger.info("Pass 3 found ${pass3Results.size} contradictions")
    }
    
    /**
     * Pass 4: Run linguistic drift detection on each speaker's statements.
     */
    private fun runPass4LinguisticDriftDetection() {
        pass4Results.clear()
        
        // Get entity profiles as map
        val profilesMap = entityDetector.getAllProfiles().associateBy { it.name }
        
        // Detect behavioral anomalies
        val anomalies = linguisticDetector.detectBehavioralAnomalies(statementIndex, profilesMap)
        allAnomalies.addAll(anomalies)
        
        // Convert behavioral anomalies to contradictions
        val behavioralContradictions = linguisticDetector.convertToContradictions(anomalies, statementIndex)
        pass4Results.addAll(behavioralContradictions)
        
        logger.info("Pass 4 found ${pass4Results.size} behavioral contradictions")
    }
    
    /**
     * Check for contradiction between two statements using semantic embeddings.
     */
    private fun checkForContradiction(
        statementA: Statement,
        statementB: Statement,
        type: ContradictionType
    ): Contradiction? {
        val embeddingA = statementA.embedding ?: return null
        val embeddingB = statementB.embedding ?: return null
        
        val match = semanticGenerator.detectSemanticContradiction(
            embeddingA, embeddingB,
            statementA.text, statementB.text,
            statementA.sentiment, statementB.sentiment
        )
        
        return match?.let {
            Contradiction(
                type = type,
                sourceStatement = statementA,
                targetStatement = statementB,
                sourceDocument = statementA.documentId,
                sourceLineNumber = statementA.lineNumber,
                severity = calculateSeverity(it.contradictionScore),
                description = "${it.reason}: '${statementA.text.take(50)}...' vs '${statementB.text.take(50)}...'",
                legalTrigger = determineLegalTrigger(statementA, statementB, it),
                affectedEntities = listOf(statementA.speaker, statementB.speaker).distinct(),
                similarityScore = it.similarity
            )
        }
    }
    
    /**
     * Cross-check timeline events against statement claims.
     */
    private fun crossCheckTimelineWithStatements(): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        val events = timelineDetector.getAllEvents()
        val statements = statementIndex.getAllStatements()
        
        // Check for statements that claim events happened at times that conflict with timeline
        for (event in events) {
            for (statement in statements) {
                // Check if statement references the event but with wrong timing
                if (referencesEvent(statement, event) && hasTimingConflict(statement, event)) {
                    contradictions.add(
                        Contradiction(
                            type = ContradictionType.TIMELINE,
                            sourceStatement = statement,
                            targetStatement = Statement(
                                id = event.id,
                                speaker = event.entityIds.firstOrNull() ?: "Unknown",
                                text = event.description,
                                documentId = event.documentId,
                                documentName = event.documentId,
                                lineNumber = 0,
                                timestamp = event.timestamp,
                                timestampMillis = event.timestampMillis
                            ),
                            sourceDocument = statement.documentId,
                            sourceLineNumber = statement.lineNumber,
                            severity = 6,
                            description = "Statement claims event at different time than documented: " +
                                "'${statement.text.take(40)}...'",
                            legalTrigger = LegalTrigger.TIMELINE_INCONSISTENCY,
                            affectedEntities = (listOf(statement.speaker) + event.entityIds).distinct()
                        )
                    )
                }
            }
        }
        
        return contradictions
    }
    
    /**
     * Check if a statement references an event.
     */
    private fun referencesEvent(statement: Statement, event: TimelineEvent): Boolean {
        val statementWords = statement.text.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val eventWords = event.description.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        
        val intersection = statementWords.intersect(eventWords)
        return intersection.size >= 3
    }
    
    /**
     * Check for timing conflict between statement and event.
     */
    private fun hasTimingConflict(statement: Statement, event: TimelineEvent): Boolean {
        val statementTime = statement.timestampMillis ?: return false
        val eventTime = event.timestampMillis
        
        // More than threshold days difference is a conflict
        val diffDays = kotlin.math.abs(statementTime - eventTime) / MILLIS_PER_DAY
        return diffDays > DAY_DIFFERENCE_THRESHOLD_DAYS
    }
    
    /**
     * Calculate severity from contradiction score.
     */
    private fun calculateSeverity(contradictionScore: Double): Int {
        return when {
            contradictionScore > 0.9 -> 10
            contradictionScore > 0.8 -> 9
            contradictionScore > 0.7 -> 8
            contradictionScore > 0.6 -> 7
            contradictionScore > 0.5 -> 6
            else -> 5
        }
    }
    
    /**
     * Determine appropriate legal trigger for a contradiction.
     */
    private fun determineLegalTrigger(
        statementA: Statement,
        statementB: Statement,
        match: ContradictionMatch
    ): LegalTrigger {
        return when {
            match.reason.contains("negation", ignoreCase = true) -> LegalTrigger.MISREPRESENTATION
            match.reason.contains("conflicting", ignoreCase = true) -> LegalTrigger.FRAUD
            statementA.speaker == statementB.speaker -> LegalTrigger.UNRELIABLE_TESTIMONY
            statementA.documentId != statementB.documentId -> LegalTrigger.MISREPRESENTATION
            else -> LegalTrigger.CONCEALMENT
        }
    }
    
    /**
     * Build entity involvement map.
     */
    private fun buildEntityInvolvementMap(
        contradictions: List<Contradiction>
    ): Map<String, EntityInvolvement> {
        val involvementMap = mutableMapOf<String, MutableList<String>>()
        
        for (contradiction in contradictions) {
            for (entityId in contradiction.affectedEntities) {
                involvementMap.getOrPut(entityId) { mutableListOf() }.add(contradiction.id)
            }
        }
        
        return involvementMap.mapValues { (entityId, contradictionIds) ->
            val profile = entityDetector.getProfileByName(entityId)
            EntityInvolvement(
                entityId = entityId,
                entityName = profile?.name ?: entityId,
                contradictionCount = contradictionIds.size,
                contradictionIds = contradictionIds,
                liabilityScore = calculateLiabilityScore(entityId, contradictionIds, contradictions),
                primaryRole = profile?.role ?: "Unknown"
            )
        }
    }
    
    /**
     * Calculate liability score for an entity.
     */
    private fun calculateLiabilityScore(
        entityId: String,
        contradictionIds: List<String>,
        allContradictions: List<Contradiction>
    ): Int {
        val entityContradictions = allContradictions.filter { it.id in contradictionIds }
        val avgSeverity = entityContradictions.map { it.severity }.average()
        val count = entityContradictions.size
        
        // Score based on count and severity
        val baseScore = (avgSeverity * 5 + count * 3).toInt()
        return baseScore.coerceIn(0, 100)
    }
    
    /**
     * Build document links map.
     */
    private fun buildDocumentLinks(
        contradictions: List<Contradiction>
    ): Map<String, List<String>> {
        val documentMap = mutableMapOf<String, MutableList<String>>()
        
        for (contradiction in contradictions) {
            documentMap.getOrPut(contradiction.sourceDocument) { mutableListOf() }
                .add(contradiction.id)
        }
        
        return documentMap
    }
    
    /**
     * Build legal trigger evidence.
     */
    private fun buildLegalTriggerEvidence(
        contradictions: List<Contradiction>
    ): List<LegalTriggerEvidence> {
        val triggerMap = contradictions
            .filter { it.legalTrigger != null }
            .groupBy { it.legalTrigger!! }
        
        return triggerMap.map { (trigger, triggeredContradictions) ->
            LegalTriggerEvidence(
                trigger = trigger,
                description = getTriggerdescription(trigger),
                supportingContradictionIds = triggeredContradictions.map { it.id },
                confidence = calculateTriggerConfidence(triggeredContradictions),
                recommendation = getTriggerRecommendation(trigger)
            )
        }
    }
    
    /**
     * Get description for a legal trigger.
     */
    private fun getTriggerdescription(trigger: LegalTrigger): String {
        return when (trigger) {
            LegalTrigger.FRAUD -> "Evidence suggests intentional deception for personal gain"
            LegalTrigger.MISREPRESENTATION -> "Statements contain false or misleading information"
            LegalTrigger.CONCEALMENT -> "Information appears to be deliberately hidden"
            LegalTrigger.PERJURY_RISK -> "Statements under oath may be false"
            LegalTrigger.BREACH_OF_CONTRACT -> "Contractual obligations appear violated"
            LegalTrigger.TIMELINE_INCONSISTENCY -> "Chronological claims are inconsistent"
            LegalTrigger.UNRELIABLE_TESTIMONY -> "Testimony shows internal contradictions"
            LegalTrigger.FINANCIAL_DISCREPANCY -> "Financial figures are inconsistent"
            LegalTrigger.CONFLICT_OF_INTEREST -> "Potential conflict of interest detected"
            LegalTrigger.NEGLIGENCE -> "Duty of care may have been breached"
        }
    }
    
    /**
     * Get recommendation for a legal trigger.
     */
    private fun getTriggerRecommendation(trigger: LegalTrigger): String {
        return when (trigger) {
            LegalTrigger.FRAUD -> "Consider further investigation and potential fraud charges"
            LegalTrigger.MISREPRESENTATION -> "Document all misrepresentations for legal proceedings"
            LegalTrigger.CONCEALMENT -> "Request full disclosure of all relevant documents"
            LegalTrigger.PERJURY_RISK -> "Compare statements under oath with documented evidence"
            LegalTrigger.BREACH_OF_CONTRACT -> "Review contract terms and document breaches"
            LegalTrigger.TIMELINE_INCONSISTENCY -> "Verify all dates with independent sources"
            LegalTrigger.UNRELIABLE_TESTIMONY -> "Consider impeachment of witness testimony"
            LegalTrigger.FINANCIAL_DISCREPANCY -> "Request financial audit and documentation"
            LegalTrigger.CONFLICT_OF_INTEREST -> "Investigate relationships and potential bias"
            LegalTrigger.NEGLIGENCE -> "Document standard of care and deviations"
        }
    }
    
    /**
     * Calculate confidence for a trigger.
     */
    private fun calculateTriggerConfidence(contradictions: List<Contradiction>): Double {
        if (contradictions.isEmpty()) return 0.0
        val avgSeverity = contradictions.map { it.severity }.average()
        return (avgSeverity / 10.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * Generate summary of findings.
     */
    private fun generateSummary(
        contradictions: List<Contradiction>,
        anomalies: List<BehavioralAnomaly>,
        conflicts: List<TimelineConflict>
    ): String {
        val builder = StringBuilder()
        
        builder.append("Analysis Summary:\n\n")
        builder.append("Total contradictions detected: ${contradictions.size}\n")
        builder.append("Behavioral anomalies: ${anomalies.size}\n")
        builder.append("Timeline conflicts: ${conflicts.size}\n\n")
        
        // By type
        val byType = contradictions.groupBy { it.type }
        builder.append("Contradictions by type:\n")
        for ((type, typeContradictions) in byType) {
            builder.append("  - $type: ${typeContradictions.size}\n")
        }
        
        // Critical findings
        val critical = contradictions.filter { it.severity >= 8 }
        if (critical.isNotEmpty()) {
            builder.append("\nCritical findings (${critical.size}):\n")
            for (contradiction in critical.take(5)) {
                builder.append("  - ${contradiction.description.take(100)}...\n")
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Self-verification step.
     */
    private fun selfVerify(): VerificationStatus {
        warnings.clear()
        autoCorrections.clear()
        
        // Check 1: Statements indexed
        val statementsIndexed = statementIndex.hasStatements()
        if (!statementsIndexed) {
            warnings.add("No statements were indexed")
        }
        
        // Check 2: Embeddings generated
        val embeddingsGenerated = statementIndex.hasEmbeddings()
        if (!embeddingsGenerated && statementsIndexed) {
            warnings.add("Embeddings not generated - auto-generating")
            autoCorrections.add("Auto-generated embeddings")
            generateEmbeddings()
        }
        
        // Check 3: Timeline objects exist
        val timelineExists = timelineDetector.hasTimelineObjects()
        if (!timelineExists && statementsIndexed) {
            warnings.add("Timeline not built - auto-building")
            autoCorrections.add("Auto-built timeline")
            buildTimeline()
        }
        
        // Check 4: Entity profiles exist
        val entityProfilesExist = entityDetector.hasProfiles()
        if (!entityProfilesExist && statementsIndexed) {
            warnings.add("Entity profiles not built - auto-building")
            autoCorrections.add("Auto-built entity profiles")
            buildEntityProfiles()
        }
        
        // Check 5 is verified after passes run
        val contradictionPassRan = pass1Results.isNotEmpty() || pass2Results.isNotEmpty() ||
            pass3Results.isNotEmpty() || pass4Results.isNotEmpty()
        
        return VerificationStatus(
            statementsIndexed = statementsIndexed,
            embeddingsGenerated = statementIndex.hasEmbeddings(),
            timelineObjectsExist = timelineDetector.hasTimelineObjects(),
            entityProfilesExist = entityDetector.hasProfiles(),
            contradictionPassRan = contradictionPassRan,
            warnings = warnings.toList(),
            autoCorrections = autoCorrections.toList()
        )
    }
    
    /**
     * Reset the engine for a new analysis.
     */
    fun reset() {
        statementIndex.clear()
        entityDetector.clear()
        timelineDetector.clear()
        pass1Results.clear()
        pass2Results.clear()
        pass3Results.clear()
        pass4Results.clear()
        allAnomalies.clear()
        allTimelineConflicts.clear()
        warnings.clear()
        autoCorrections.clear()
    }
}
