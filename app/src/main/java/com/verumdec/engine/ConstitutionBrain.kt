package com.verumdec.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.verumdec.data.*
import java.security.MessageDigest
import java.util.*

/**
 * ConstitutionBrain - Enforces the Verum Omnis Constitutional Charter.
 * 
 * This class is responsible for loading the constitution rules and enforcing them
 * throughout the analysis pipeline. Any violation of constitutional rules will be
 * detected and reported.
 * 
 * ## Rules Enforced:
 * - No bias in entity treatment
 * - No missing admissions
 * - No contradiction skipping
 * - No ambiguity allowed in evidence
 * - Must show all evidence origins
 * - Must show hash chain
 */
class ConstitutionBrain(private val context: Context) {

    private var constitution: Constitution? = null
    private val gson = Gson()
    
    companion object {
        private const val CONSTITUTION_FILE = "constitution.json"
        
        /**
         * Wrapper function to enforce constitution rules on any analysis result.
         * This should be called by all Brain components after their analysis.
         */
        fun <T> enforceConstitution(
            brain: ConstitutionBrain,
            result: T,
            evidence: List<Evidence>,
            entities: List<Entity>,
            contradictions: List<Contradiction> = emptyList(),
            statements: List<Statement> = emptyList()
        ): ConstitutionEnforcementResult {
            return brain.enforce(
                evidence = evidence,
                entities = entities,
                contradictions = contradictions,
                statements = statements
            )
        }
    }

    init {
        loadConstitution()
    }

    /**
     * Load the constitution from the assets folder.
     */
    private fun loadConstitution() {
        try {
            val jsonString = context.assets.open(CONSTITUTION_FILE).bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            
            val rules = mutableListOf<ConstitutionRule>()
            val rulesArray = jsonObject.getAsJsonArray("rules")
            
            for (ruleElement in rulesArray) {
                val rule = ruleElement.asJsonObject
                rules.add(ConstitutionRule(
                    id = rule.get("id").asString,
                    name = rule.get("name").asString,
                    description = rule.get("description").asString,
                    severity = ConstitutionViolationSeverity.valueOf(rule.get("severity").asString),
                    checkType = ConstitutionCheckType.valueOf(rule.get("checkType").asString)
                ))
            }
            
            val metadata = jsonObject.getAsJsonObject("metadata")
            
            constitution = Constitution(
                version = jsonObject.get("version").asString,
                name = jsonObject.get("name").asString,
                description = jsonObject.get("description").asString,
                rules = rules,
                metadata = ConstitutionMetadata(
                    createdAt = metadata.get("createdAt")?.asString ?: "",
                    lastModified = metadata.get("lastModified")?.asString ?: "",
                    author = metadata.get("author")?.asString ?: ""
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Create default constitution if loading fails
            constitution = createDefaultConstitution()
        }
    }

    /**
     * Create a default constitution if the JSON file cannot be loaded.
     */
    private fun createDefaultConstitution(): Constitution {
        return Constitution(
            version = "1.0.0",
            name = "Default Constitution",
            description = "Default constitutional rules",
            rules = listOf(
                ConstitutionRule("no_bias", "No Bias", 
                    "Analysis must not show bias toward any entity", 
                    ConstitutionViolationSeverity.CRITICAL, ConstitutionCheckType.BIAS_CHECK),
                ConstitutionRule("no_missing_admissions", "No Missing Admissions", 
                    "All admissions must be reported", 
                    ConstitutionViolationSeverity.CRITICAL, ConstitutionCheckType.ADMISSION_CHECK),
                ConstitutionRule("no_contradiction_skip", "No Contradiction Skip", 
                    "All contradictions must be included", 
                    ConstitutionViolationSeverity.CRITICAL, ConstitutionCheckType.CONTRADICTION_CHECK),
                ConstitutionRule("no_ambiguity_in_evidence", "No Ambiguity Allowed in Evidence", 
                    "Evidence must be clearly attributed", 
                    ConstitutionViolationSeverity.HIGH, ConstitutionCheckType.AMBIGUITY_CHECK),
                ConstitutionRule("show_evidence_origins", "Must Show All Evidence Origins", 
                    "Every evidence must have documented origin", 
                    ConstitutionViolationSeverity.HIGH, ConstitutionCheckType.ORIGIN_CHECK),
                ConstitutionRule("show_hash_chain", "Must Show Hash Chain", 
                    "Hash chain must be maintained", 
                    ConstitutionViolationSeverity.HIGH, ConstitutionCheckType.HASH_CHAIN_CHECK)
            )
        )
    }

    /**
     * Get the loaded constitution.
     */
    fun getConstitution(): Constitution? = constitution

    /**
     * Enforce all constitution rules and return any violations.
     */
    fun enforce(
        evidence: List<Evidence>,
        entities: List<Entity>,
        contradictions: List<Contradiction> = emptyList(),
        statements: List<Statement> = emptyList()
    ): ConstitutionEnforcementResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val checkedRules = mutableListOf<String>()
        
        val rules = constitution?.rules ?: return ConstitutionEnforcementResult(
            isCompliant = true,
            violations = emptyList(),
            checkedRules = emptyList()
        )
        
        for (rule in rules) {
            checkedRules.add(rule.id)
            
            val ruleViolations = when (rule.checkType) {
                ConstitutionCheckType.BIAS_CHECK -> checkForBias(rule, entities)
                ConstitutionCheckType.ADMISSION_CHECK -> checkForMissingAdmissions(rule, statements, contradictions)
                ConstitutionCheckType.CONTRADICTION_CHECK -> checkForSkippedContradictions(rule, statements, contradictions)
                ConstitutionCheckType.AMBIGUITY_CHECK -> checkForAmbiguity(rule, evidence)
                ConstitutionCheckType.ORIGIN_CHECK -> checkForMissingOrigins(rule, evidence)
                ConstitutionCheckType.HASH_CHAIN_CHECK -> checkHashChain(rule, evidence)
            }
            
            violations.addAll(ruleViolations)
        }
        
        return ConstitutionEnforcementResult(
            isCompliant = violations.none { it.severity == ConstitutionViolationSeverity.CRITICAL },
            violations = violations,
            checkedRules = checkedRules
        )
    }

    /**
     * Check for bias in entity treatment.
     * Ensures all entities are evaluated using the same criteria.
     */
    private fun checkForBias(rule: ConstitutionRule, entities: List<Entity>): List<ConstitutionViolation> {
        val violations = mutableListOf<ConstitutionViolation>()
        
        if (entities.isEmpty()) return violations
        
        // Check for unequal treatment based on statement count
        val statementsPerEntity = entities.map { it.id to it.statements.size }
        val avgStatements = if (statementsPerEntity.isNotEmpty()) {
            statementsPerEntity.map { it.second }.average()
        } else 0.0
        
        // Check if liability scores are assigned fairly (if calculated)
        val scoredEntities = entities.filter { it.liabilityScore > 0 }
        val unscoredEntities = entities.filter { it.liabilityScore == 0f && it.statements.isNotEmpty() }
        
        if (scoredEntities.isNotEmpty() && unscoredEntities.isNotEmpty()) {
            for (entity in unscoredEntities) {
                violations.add(ConstitutionViolation(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Entity ${entity.primaryName} has statements but no liability score",
                    severity = rule.severity,
                    checkType = rule.checkType,
                    affectedEntityId = entity.id,
                    details = "Potential bias: entity with ${entity.statements.size} statements was not scored while others were"
                ))
            }
        }
        
        return violations
    }

    /**
     * Check for missing admissions in the analysis.
     */
    private fun checkForMissingAdmissions(
        rule: ConstitutionRule, 
        statements: List<Statement>,
        contradictions: List<Contradiction>
    ): List<ConstitutionViolation> {
        val violations = mutableListOf<ConstitutionViolation>()
        
        // Get all admission statements
        val admissions = statements.filter { it.type == StatementType.ADMISSION }
        
        // Check if any admission is not referenced in contradictions when there's a related denial
        for (admission in admissions) {
            val entityDenials = statements.filter { 
                it.entityId == admission.entityId && it.type == StatementType.DENIAL 
            }
            
            // If entity has both admissions and denials, there should be a contradiction
            if (entityDenials.isNotEmpty()) {
                val hasContradiction = contradictions.any { c ->
                    (c.statementA.id == admission.id || c.statementB.id == admission.id)
                }
                
                if (!hasContradiction) {
                    // Check if keywords overlap
                    val admissionKeywords = admission.keywords.toSet()
                    val overlappingDenials = entityDenials.filter { denial ->
                        denial.keywords.any { it in admissionKeywords }
                    }
                    
                    if (overlappingDenials.isNotEmpty()) {
                        violations.add(ConstitutionViolation(
                            ruleId = rule.id,
                            ruleName = rule.name,
                            description = "Admission not analyzed against related denial",
                            severity = rule.severity,
                            checkType = rule.checkType,
                            affectedEntityId = admission.entityId,
                            details = "Admission '${admission.text.take(50)}...' has related denials but no contradiction was detected"
                        ))
                    }
                }
            }
        }
        
        return violations
    }

    /**
     * Check for skipped contradictions.
     */
    private fun checkForSkippedContradictions(
        rule: ConstitutionRule,
        statements: List<Statement>,
        contradictions: List<Contradiction>
    ): List<ConstitutionViolation> {
        val violations = mutableListOf<ConstitutionViolation>()
        
        // Group statements by entity
        val statementsByEntity = statements.groupBy { it.entityId }
        
        for ((entityId, entityStatements) in statementsByEntity) {
            // Check for claim/denial pairs that should generate contradictions
            val claims = entityStatements.filter { it.type == StatementType.CLAIM }
            val denials = entityStatements.filter { it.type == StatementType.DENIAL }
            
            for (claim in claims) {
                for (denial in denials) {
                    // Check if they share significant keywords
                    val commonKeywords = claim.keywords.intersect(denial.keywords.toSet())
                    
                    if (commonKeywords.size >= 2) {
                        // This should be a contradiction - check if it exists
                        val existingContradiction = contradictions.any { c ->
                            (c.statementA.id == claim.id && c.statementB.id == denial.id) ||
                            (c.statementA.id == denial.id && c.statementB.id == claim.id)
                        }
                        
                        if (!existingContradiction) {
                            violations.add(ConstitutionViolation(
                                ruleId = rule.id,
                                ruleName = rule.name,
                                description = "Potential contradiction skipped between claim and denial",
                                severity = rule.severity,
                                checkType = rule.checkType,
                                affectedEntityId = entityId,
                                details = "Claim and denial share keywords (${commonKeywords.take(3).joinToString()}) but no contradiction was recorded"
                            ))
                        }
                    }
                }
            }
        }
        
        return violations
    }

    /**
     * Check for ambiguous evidence.
     */
    private fun checkForAmbiguity(rule: ConstitutionRule, evidence: List<Evidence>): List<ConstitutionViolation> {
        val violations = mutableListOf<ConstitutionViolation>()
        
        for (item in evidence) {
            // Check for missing or ambiguous metadata
            val hasCreator = !item.metadata.author.isNullOrBlank() || 
                            !item.metadata.sender.isNullOrBlank()
            val hasDate = item.metadata.creationDate != null
            
            // Evidence must have clear attribution
            if (!hasCreator && item.processed) {
                violations.add(ConstitutionViolation(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Evidence has no clear author/sender attribution",
                    severity = rule.severity,
                    checkType = rule.checkType,
                    affectedEvidenceId = item.id,
                    details = "File '${item.fileName}' lacks author/sender information making attribution ambiguous"
                ))
            }
            
            // Check for suspiciously empty extracted text on processed items
            if (item.processed && item.extractedText.length < 10 && item.type != EvidenceType.IMAGE) {
                violations.add(ConstitutionViolation(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Evidence extraction result is ambiguous",
                    severity = ConstitutionViolationSeverity.MEDIUM,
                    checkType = rule.checkType,
                    affectedEvidenceId = item.id,
                    details = "File '${item.fileName}' processed but yielded very little text content"
                ))
            }
        }
        
        return violations
    }

    /**
     * Check for missing evidence origins.
     */
    private fun checkForMissingOrigins(rule: ConstitutionRule, evidence: List<Evidence>): List<ConstitutionViolation> {
        val violations = mutableListOf<ConstitutionViolation>()
        
        for (item in evidence) {
            // Check if origin is documented
            if (item.origin.isBlank() && item.processed) {
                violations.add(ConstitutionViolation(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Evidence origin not documented",
                    severity = rule.severity,
                    checkType = rule.checkType,
                    affectedEvidenceId = item.id,
                    details = "File '${item.fileName}' has no origin tracking information"
                ))
            }
            
            // Also check filePath as fallback origin
            if (item.filePath.isBlank()) {
                violations.add(ConstitutionViolation(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Evidence file path is missing",
                    severity = rule.severity,
                    checkType = rule.checkType,
                    affectedEvidenceId = item.id,
                    details = "File '${item.fileName}' has no file path recorded"
                ))
            }
        }
        
        return violations
    }

    /**
     * Check for hash chain integrity.
     */
    private fun checkHashChain(rule: ConstitutionRule, evidence: List<Evidence>): List<ConstitutionViolation> {
        val violations = mutableListOf<ConstitutionViolation>()
        
        for (item in evidence) {
            // Check if content hash is present for processed evidence
            if (item.processed && item.contentHash.isBlank()) {
                violations.add(ConstitutionViolation(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Evidence content hash missing",
                    severity = rule.severity,
                    checkType = rule.checkType,
                    affectedEvidenceId = item.id,
                    details = "File '${item.fileName}' has no content hash for integrity verification"
                ))
            }
        }
        
        return violations
    }

    /**
     * Calculate SHA-256 hash for a given content.
     */
    fun calculateHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify a hash chain for evidence integrity.
     */
    fun verifyHashChain(evidence: List<Evidence>): Boolean {
        for (item in evidence) {
            if (item.processed && item.contentHash.isNotBlank()) {
                val calculatedHash = calculateHash(item.extractedText)
                if (calculatedHash != item.contentHash) {
                    return false
                }
            }
        }
        return true
    }
}
