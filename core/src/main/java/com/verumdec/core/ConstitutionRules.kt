package com.verumdec.core

/**
 * ConstitutionRules - Defines the constitutional rules for the Verumdec engine.
 * 
 * This object provides the core rule definitions and validation logic that is
 * used by the ConstitutionBrain to enforce rules across all analysis modules.
 * 
 * ## Rule Categories:
 * 1. Bias Prevention - Ensures equal treatment of all entities
 * 2. Admission Tracking - Ensures all admissions are captured
 * 3. Contradiction Integrity - Ensures no contradictions are skipped
 * 4. Evidence Clarity - Ensures evidence attribution is clear
 * 5. Origin Tracking - Ensures evidence origins are documented
 * 6. Hash Chain - Ensures cryptographic integrity
 * 
 * @see com.verumdec.engine.ConstitutionBrain
 */
object ConstitutionRules {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "constitution_rules"

    /**
     * Rule IDs - Unique identifiers for each constitutional rule.
     */
    object RuleIds {
        const val NO_BIAS = "no_bias"
        const val NO_MISSING_ADMISSIONS = "no_missing_admissions"
        const val NO_CONTRADICTION_SKIP = "no_contradiction_skip"
        const val NO_AMBIGUITY_IN_EVIDENCE = "no_ambiguity_in_evidence"
        const val SHOW_EVIDENCE_ORIGINS = "show_evidence_origins"
        const val SHOW_HASH_CHAIN = "show_hash_chain"
    }

    /**
     * Rule descriptions for human-readable output.
     */
    object RuleDescriptions {
        const val NO_BIAS = "Analysis must not show bias toward any entity. All entities must be evaluated using the same criteria and thresholds."
        const val NO_MISSING_ADMISSIONS = "All admissions detected in evidence must be reported and cannot be skipped or omitted from the analysis."
        const val NO_CONTRADICTION_SKIP = "All detected contradictions must be included in the final report. Contradictions cannot be silently ignored."
        const val NO_AMBIGUITY_IN_EVIDENCE = "Evidence must be clearly attributed and sourced. Ambiguous evidence without clear origin must be flagged."
        const val SHOW_EVIDENCE_ORIGINS = "Every piece of evidence used in the analysis must have its origin clearly documented and traceable."
        const val SHOW_HASH_CHAIN = "A cryptographic hash chain must be maintained for evidence integrity verification."
    }

    /**
     * Severity levels for rule violations.
     */
    enum class Severity {
        CRITICAL,   // Analysis cannot proceed
        HIGH,       // Significant issue
        MEDIUM,     // Moderate issue
        LOW         // Minor issue
    }

    /**
     * Check types for categorizing rule enforcement.
     */
    enum class CheckType {
        BIAS_CHECK,
        ADMISSION_CHECK,
        CONTRADICTION_CHECK,
        AMBIGUITY_CHECK,
        ORIGIN_CHECK,
        HASH_CHAIN_CHECK
    }

    /**
     * Data class representing a constitutional rule.
     */
    data class Rule(
        val id: String,
        val name: String,
        val description: String,
        val severity: Severity,
        val checkType: CheckType
    )

    /**
     * Get all defined constitutional rules.
     */
    fun getAllRules(): List<Rule> {
        return listOf(
            Rule(
                id = RuleIds.NO_BIAS,
                name = "No Bias",
                description = RuleDescriptions.NO_BIAS,
                severity = Severity.CRITICAL,
                checkType = CheckType.BIAS_CHECK
            ),
            Rule(
                id = RuleIds.NO_MISSING_ADMISSIONS,
                name = "No Missing Admissions",
                description = RuleDescriptions.NO_MISSING_ADMISSIONS,
                severity = Severity.CRITICAL,
                checkType = CheckType.ADMISSION_CHECK
            ),
            Rule(
                id = RuleIds.NO_CONTRADICTION_SKIP,
                name = "No Contradiction Skip",
                description = RuleDescriptions.NO_CONTRADICTION_SKIP,
                severity = Severity.CRITICAL,
                checkType = CheckType.CONTRADICTION_CHECK
            ),
            Rule(
                id = RuleIds.NO_AMBIGUITY_IN_EVIDENCE,
                name = "No Ambiguity Allowed in Evidence",
                description = RuleDescriptions.NO_AMBIGUITY_IN_EVIDENCE,
                severity = Severity.HIGH,
                checkType = CheckType.AMBIGUITY_CHECK
            ),
            Rule(
                id = RuleIds.SHOW_EVIDENCE_ORIGINS,
                name = "Must Show All Evidence Origins",
                description = RuleDescriptions.SHOW_EVIDENCE_ORIGINS,
                severity = Severity.HIGH,
                checkType = CheckType.ORIGIN_CHECK
            ),
            Rule(
                id = RuleIds.SHOW_HASH_CHAIN,
                name = "Must Show Hash Chain",
                description = RuleDescriptions.SHOW_HASH_CHAIN,
                severity = Severity.HIGH,
                checkType = CheckType.HASH_CHAIN_CHECK
            )
        )
    }

    /**
     * Get a rule by its ID.
     */
    fun getRuleById(id: String): Rule? {
        return getAllRules().find { it.id == id }
    }

    /**
     * Get rules by severity level.
     */
    fun getRulesBySeverity(severity: Severity): List<Rule> {
        return getAllRules().filter { it.severity == severity }
    }

    /**
     * Get rules by check type.
     */
    fun getRulesByCheckType(checkType: CheckType): List<Rule> {
        return getAllRules().filter { it.checkType == checkType }
    }

    /**
     * Validate that all required rules are defined.
     */
    fun validateRules(): Boolean {
        val requiredRuleIds = listOf(
            RuleIds.NO_BIAS,
            RuleIds.NO_MISSING_ADMISSIONS,
            RuleIds.NO_CONTRADICTION_SKIP,
            RuleIds.NO_AMBIGUITY_IN_EVIDENCE,
            RuleIds.SHOW_EVIDENCE_ORIGINS,
            RuleIds.SHOW_HASH_CHAIN
        )
        
        val definedRuleIds = getAllRules().map { it.id }
        return requiredRuleIds.all { it in definedRuleIds }
    }

    /**
     * Initialize the constitution rules module.
     */
    fun initialize() {
        if (!validateRules()) {
            throw IllegalStateException("Constitutional rules validation failed. Not all required rules are defined.")
        }
    }
}
