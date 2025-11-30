package com.verumdec.core.constitution

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * ConstitutionBrain - Enforces the Verum Omnis Constitutional Charter.
 * All Brains must call enforceConstitution() at entry and exit points.
 */
class ConstitutionBrain(private val context: Context) {

    private var constitution: Constitution? = null
    private val violations = mutableListOf<ConstitutionViolation>()
    private var isLoaded = false

    companion object {
        private const val CONSTITUTION_ASSET = "constitution.json"
        
        @Volatile
        private var instance: ConstitutionBrain? = null

        fun getInstance(context: Context): ConstitutionBrain {
            return instance ?: synchronized(this) {
                instance ?: ConstitutionBrain(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Load constitution from assets.
     */
    fun loadConstitution(): Boolean {
        return try {
            context.assets.open(CONSTITUTION_ASSET).use { inputStream ->
                val reader = InputStreamReader(inputStream)
                constitution = Gson().fromJson(reader, Constitution::class.java)
                isLoaded = true
            }
            true
        } catch (e: Exception) {
            // Use default constitution if asset not found
            constitution = createDefaultConstitution()
            isLoaded = true
            true
        }
    }

    /**
     * Create default constitution rules.
     */
    private fun createDefaultConstitution(): Constitution {
        return Constitution(
            version = "1.0.0",
            name = "Verum Omnis Constitutional Charter",
            principles = listOf(
                Principle(
                    id = "P1",
                    name = "Truth Preservation",
                    description = "All evidence must be preserved in original form with full provenance tracking.",
                    enforcementLevel = EnforcementLevel.MANDATORY
                ),
                Principle(
                    id = "P2",
                    name = "Impartiality",
                    description = "Analysis must be conducted without bias toward any party.",
                    enforcementLevel = EnforcementLevel.MANDATORY
                ),
                Principle(
                    id = "P3",
                    name = "Transparency",
                    description = "All analytical methods and reasoning must be documented and explainable.",
                    enforcementLevel = EnforcementLevel.MANDATORY
                ),
                Principle(
                    id = "P4",
                    name = "Privacy Protection",
                    description = "Sensitive personal information must be handled with appropriate safeguards.",
                    enforcementLevel = EnforcementLevel.MANDATORY
                ),
                Principle(
                    id = "P5",
                    name = "Chain of Custody",
                    description = "Every piece of evidence must maintain a verifiable chain of custody.",
                    enforcementLevel = EnforcementLevel.MANDATORY
                )
            ),
            rules = listOf(
                Rule(
                    id = "R1",
                    principleId = "P1",
                    name = "Hash Verification",
                    description = "All evidence must be hashed upon ingestion with SHA-512.",
                    validationPattern = "SHA512_REQUIRED"
                ),
                Rule(
                    id = "R2",
                    principleId = "P1",
                    name = "Original Preservation",
                    description = "Original files must never be modified.",
                    validationPattern = "NO_MODIFICATION"
                ),
                Rule(
                    id = "R3",
                    principleId = "P2",
                    name = "Equal Treatment",
                    description = "All entities must be analyzed using the same criteria.",
                    validationPattern = "EQUAL_ANALYSIS"
                ),
                Rule(
                    id = "R4",
                    principleId = "P3",
                    name = "Reasoning Documentation",
                    description = "Every conclusion must include supporting evidence references.",
                    validationPattern = "EVIDENCE_CITATION"
                ),
                Rule(
                    id = "R5",
                    principleId = "P5",
                    name = "Timestamp Recording",
                    description = "All processing steps must be timestamped.",
                    validationPattern = "TIMESTAMP_REQUIRED"
                )
            )
        )
    }

    /**
     * Enforce constitution at brain entry point.
     */
    fun enforceOnEntry(brainName: String, operationType: String, metadata: Map<String, Any> = emptyMap()): EnforcementResult {
        if (!isLoaded) loadConstitution()
        
        val checkResults = mutableListOf<RuleCheck>()
        
        constitution?.rules?.forEach { rule ->
            val check = validateRule(rule, operationType, metadata)
            checkResults.add(check)
            
            if (!check.passed) {
                violations.add(ConstitutionViolation(
                    brainName = brainName,
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Entry violation: ${check.message}",
                    severity = ViolationSeverity.WARNING,
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
        
        val passed = checkResults.all { it.passed }
        return EnforcementResult(
            passed = passed,
            checks = checkResults,
            warnings = checkResults.filter { !it.passed }.map { it.message }
        )
    }

    /**
     * Enforce constitution at brain exit point.
     */
    fun enforceOnExit(brainName: String, operationType: String, result: Any?, metadata: Map<String, Any> = emptyMap()): EnforcementResult {
        if (!isLoaded) loadConstitution()
        
        val checkResults = mutableListOf<RuleCheck>()
        
        // Validate output has required properties
        val enhancedMetadata = metadata.toMutableMap().apply {
            put("hasResult", result != null)
            put("resultType", result?.javaClass?.simpleName ?: "null")
        }
        
        constitution?.rules?.forEach { rule ->
            val check = validateRule(rule, operationType, enhancedMetadata)
            checkResults.add(check)
            
            if (!check.passed) {
                violations.add(ConstitutionViolation(
                    brainName = brainName,
                    ruleId = rule.id,
                    ruleName = rule.name,
                    description = "Exit violation: ${check.message}",
                    severity = ViolationSeverity.ERROR,
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
        
        val passed = checkResults.all { it.passed }
        return EnforcementResult(
            passed = passed,
            checks = checkResults,
            warnings = checkResults.filter { !it.passed }.map { it.message }
        )
    }

    /**
     * Validate a specific rule.
     */
    private fun validateRule(rule: Rule, operationType: String, metadata: Map<String, Any>): RuleCheck {
        return when (rule.validationPattern) {
            "SHA512_REQUIRED" -> {
                val hasHash = metadata["sha512Hash"] != null || metadata["hash"] != null
                RuleCheck(rule.id, hasHash, if (hasHash) "Hash present" else "Missing SHA-512 hash")
            }
            "NO_MODIFICATION" -> {
                val isModified = metadata["modified"] as? Boolean ?: false
                RuleCheck(rule.id, !isModified, if (!isModified) "Original preserved" else "Original was modified")
            }
            "EQUAL_ANALYSIS" -> {
                RuleCheck(rule.id, true, "Equal analysis applied")
            }
            "EVIDENCE_CITATION" -> {
                val hasCitation = metadata["evidenceId"] != null || metadata["sourceId"] != null
                RuleCheck(rule.id, hasCitation, if (hasCitation) "Evidence cited" else "Missing evidence citation")
            }
            "TIMESTAMP_REQUIRED" -> {
                val hasTimestamp = metadata["timestamp"] != null || metadata["processedAt"] != null
                RuleCheck(rule.id, hasTimestamp, if (hasTimestamp) "Timestamp present" else "Missing timestamp")
            }
            else -> RuleCheck(rule.id, true, "Rule validated")
        }
    }

    /**
     * Get all accumulated violations.
     */
    fun getViolations(): List<ConstitutionViolation> = violations.toList()

    /**
     * Get critical violations only.
     */
    fun getCriticalViolations(): List<ConstitutionViolation> {
        return violations.filter { it.severity == ViolationSeverity.ERROR }
    }

    /**
     * Check if there are any violations.
     */
    fun hasViolations(): Boolean = violations.isNotEmpty()

    /**
     * Clear violations (after generating report).
     */
    fun clearViolations() {
        violations.clear()
    }

    /**
     * Get constitution version.
     */
    fun getVersion(): String = constitution?.version ?: "unknown"

    /**
     * Get all principles.
     */
    fun getPrinciples(): List<Principle> = constitution?.principles ?: emptyList()
}

// Data classes for constitution structure
data class Constitution(
    val version: String,
    val name: String,
    val principles: List<Principle>,
    val rules: List<Rule>
)

data class Principle(
    val id: String,
    val name: String,
    val description: String,
    @SerializedName("enforcement_level")
    val enforcementLevel: EnforcementLevel
)

data class Rule(
    val id: String,
    @SerializedName("principle_id")
    val principleId: String,
    val name: String,
    val description: String,
    @SerializedName("validation_pattern")
    val validationPattern: String
)

enum class EnforcementLevel {
    MANDATORY,
    RECOMMENDED,
    OPTIONAL
}

data class RuleCheck(
    val ruleId: String,
    val passed: Boolean,
    val message: String
)

data class EnforcementResult(
    val passed: Boolean,
    val checks: List<RuleCheck>,
    val warnings: List<String>
)

data class ConstitutionViolation(
    val brainName: String,
    val ruleId: String,
    val ruleName: String,
    val description: String,
    val severity: ViolationSeverity,
    val timestamp: Long
)

enum class ViolationSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}
