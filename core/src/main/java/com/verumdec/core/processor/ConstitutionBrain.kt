package com.verumdec.core.processor

import java.util.UUID

/**
 * ConstitutionBrain - Enforce Verum Omnis rules at each step
 *
 * Validates evidence processing and analysis against the Verum Omnis
 * constitutional charter to ensure integrity, objectivity, and compliance.
 *
 * Operates fully offline without external dependencies.
 */
class ConstitutionBrain {

    companion object {
        // Timestamp validation constants
        private const val MS_PER_DAY = 86400000L // 24 * 60 * 60 * 1000
        private const val JANUARY_1_1990_MS = 631152000000L // Unix timestamp for 1990-01-01 00:00:00 UTC

        // Verum Omnis Constitutional Rules
        private val RULES = listOf(
            Rule(
                id = "VO-001",
                name = "Evidence Integrity",
                category = RuleCategory.EVIDENCE_INTEGRITY,
                description = "All evidence must be cryptographically hashed and verified",
                checkFn = "checkEvidenceIntegrity"
            ),
            Rule(
                id = "VO-002",
                name = "Chain of Custody",
                category = RuleCategory.CHAIN_OF_CUSTODY,
                description = "Evidence handling must be documented with timestamps",
                checkFn = "checkChainOfCustody"
            ),
            Rule(
                id = "VO-003",
                name = "Analysis Objectivity",
                category = RuleCategory.ANALYSIS_OBJECTIVITY,
                description = "Analysis must be based solely on evidence without bias",
                checkFn = "checkAnalysisObjectivity"
            ),
            Rule(
                id = "VO-004",
                name = "Temporal Accuracy",
                category = RuleCategory.TEMPORAL_ACCURACY,
                description = "All timestamps must be validated and consistent",
                checkFn = "checkTemporalAccuracy"
            ),
            Rule(
                id = "VO-005",
                name = "Entity Protection",
                category = RuleCategory.ENTITY_PROTECTION,
                description = "Personal information must be handled according to privacy rules",
                checkFn = "checkEntityProtection"
            ),
            Rule(
                id = "VO-006",
                name = "Disclosure Requirements",
                category = RuleCategory.DISCLOSURE_REQUIREMENTS,
                description = "All material findings must be disclosed without selective omission",
                checkFn = "checkDisclosureRequirements"
            ),
            Rule(
                id = "VO-007",
                name = "Methodology Compliance",
                category = RuleCategory.METHODOLOGY_COMPLIANCE,
                description = "Analysis methodology must follow established forensic standards",
                checkFn = "checkMethodologyCompliance"
            ),
            Rule(
                id = "VO-008",
                name = "No Evidence Tampering",
                category = RuleCategory.EVIDENCE_INTEGRITY,
                description = "Original evidence must never be modified",
                checkFn = "checkNoTampering"
            ),
            Rule(
                id = "VO-009",
                name = "Contradiction Transparency",
                category = RuleCategory.ANALYSIS_OBJECTIVITY,
                description = "All contradictions must be reported regardless of party",
                checkFn = "checkContradictionTransparency"
            ),
            Rule(
                id = "VO-010",
                name = "Verifiable Analysis",
                category = RuleCategory.METHODOLOGY_COMPLIANCE,
                description = "All analysis steps must be reproducible and verifiable",
                checkFn = "checkVerifiableAnalysis"
            )
        )
    }

    /**
     * Internal rule definition.
     */
    private data class Rule(
        val id: String,
        val name: String,
        val category: RuleCategory,
        val description: String,
        val checkFn: String
    )

    /**
     * Validation context containing evidence and analysis state.
     */
    data class ValidationContext(
        val evidenceHashes: List<String> = emptyList(),
        val evidenceTimestamps: List<Long> = emptyList(),
        val analysisSteps: List<String> = emptyList(),
        val contradictionsFound: Int = 0,
        val contradictionsReported: Int = 0,
        val entitiesAnalyzed: Int = 0,
        val entitiesWithFindings: Int = 0,
        val originalEvidencePreserved: Boolean = true,
        val chainOfCustodyDocumented: Boolean = true,
        val methodologyDocumented: Boolean = true,
        val privacyMeasuresApplied: Boolean = true,
        val customRules: Map<String, Boolean> = emptyMap()
    )

    /**
     * Validate a processing step against Verum Omnis rules.
     *
     * @param stepName The name of the processing step
     * @param context The validation context with current state
     * @return ConstitutionBrainResult containing validation results
     */
    fun validate(stepName: String, context: ValidationContext): ConstitutionBrainResult {
        return try {
            val rulesChecked = mutableListOf<RuleValidation>()
            val violations = mutableListOf<RuleViolation>()
            val warnings = mutableListOf<RuleWarning>()
            val recommendations = mutableListOf<String>()

            // Check each rule
            for (rule in RULES) {
                val result = checkRule(rule, context)
                rulesChecked.add(result)

                if (!result.passed) {
                    violations.add(RuleViolation(
                        ruleId = rule.id,
                        ruleName = rule.name,
                        severity = determineSeverity(rule.category),
                        description = result.details,
                        remediation = generateRemediation(rule)
                    ))
                } else if (result.details.contains("warning", ignoreCase = true)) {
                    warnings.add(RuleWarning(
                        ruleId = rule.id,
                        ruleName = rule.name,
                        description = result.details,
                        recommendation = generateRecommendation(rule)
                    ))
                }
            }

            // Generate recommendations
            recommendations.addAll(generateRecommendations(violations, warnings, context))

            // Calculate compliance score
            val complianceScore = calculateComplianceScore(rulesChecked, violations)

            val compliant = violations.none { it.severity in listOf(AnomalySeverity.CRITICAL, AnomalySeverity.HIGH) }

            ConstitutionBrainResult.Success(
                compliant = compliant,
                rulesChecked = rulesChecked,
                violations = violations,
                warnings = warnings,
                recommendations = recommendations,
                complianceScore = complianceScore
            )
        } catch (e: Exception) {
            ConstitutionBrainResult.Failure(
                error = "Validation error: ${e.message}",
                errorCode = ConstitutionErrorCode.VALIDATION_FAILED
            )
        }
    }

    /**
     * Validate the entire analysis pipeline.
     *
     * @param context The complete validation context
     * @return ConstitutionBrainResult containing validation results
     */
    fun validatePipeline(context: ValidationContext): ConstitutionBrainResult {
        return validate("full_pipeline", context)
    }

    /**
     * Check a specific rule against the context.
     */
    private fun checkRule(rule: Rule, context: ValidationContext): RuleValidation {
        val (passed, details) = when (rule.checkFn) {
            "checkEvidenceIntegrity" -> checkEvidenceIntegrity(context)
            "checkChainOfCustody" -> checkChainOfCustody(context)
            "checkAnalysisObjectivity" -> checkAnalysisObjectivity(context)
            "checkTemporalAccuracy" -> checkTemporalAccuracy(context)
            "checkEntityProtection" -> checkEntityProtection(context)
            "checkDisclosureRequirements" -> checkDisclosureRequirements(context)
            "checkMethodologyCompliance" -> checkMethodologyCompliance(context)
            "checkNoTampering" -> checkNoTampering(context)
            "checkContradictionTransparency" -> checkContradictionTransparency(context)
            "checkVerifiableAnalysis" -> checkVerifiableAnalysis(context)
            else -> Pair(true, "Check not implemented")
        }

        return RuleValidation(
            ruleId = rule.id,
            ruleName = rule.name,
            category = rule.category,
            passed = passed,
            details = details
        )
    }

    /**
     * VO-001: Check evidence integrity.
     */
    private fun checkEvidenceIntegrity(context: ValidationContext): Pair<Boolean, String> {
        if (context.evidenceHashes.isEmpty()) {
            return Pair(false, "No evidence hashes found - all evidence must be cryptographically hashed")
        }

        // Check for valid hash format (SHA-512 = 128 hex chars)
        val invalidHashes = context.evidenceHashes.filter { it.length != 128 }
        if (invalidHashes.isNotEmpty()) {
            return Pair(false, "Invalid hash format detected for ${invalidHashes.size} evidence item(s)")
        }

        return Pair(true, "All evidence items are properly hashed")
    }

    /**
     * VO-002: Check chain of custody.
     */
    private fun checkChainOfCustody(context: ValidationContext): Pair<Boolean, String> {
        if (!context.chainOfCustodyDocumented) {
            return Pair(false, "Chain of custody is not documented")
        }

        if (context.evidenceTimestamps.isEmpty()) {
            return Pair(false, "No timestamps recorded for evidence handling")
        }

        // Check for chronological order
        val sorted = context.evidenceTimestamps.sorted()
        if (context.evidenceTimestamps != sorted) {
            return Pair(false, "Evidence timestamps are not in chronological order - warning: possible tampering")
        }

        return Pair(true, "Chain of custody is properly documented")
    }

    /**
     * VO-003: Check analysis objectivity.
     */
    private fun checkAnalysisObjectivity(context: ValidationContext): Pair<Boolean, String> {
        if (context.entitiesAnalyzed == 0) {
            return Pair(true, "No entities analyzed yet")
        }

        // Check if analysis covers all entities
        val coverageRatio = context.entitiesWithFindings.toFloat() / context.entitiesAnalyzed

        if (coverageRatio == 1f && context.entitiesAnalyzed > 1) {
            return Pair(true, "Warning: All entities have findings - verify objectivity")
        }

        if (coverageRatio == 0f && context.contradictionsFound > 0) {
            return Pair(false, "Contradictions found but no entities have findings - possible selective reporting")
        }

        return Pair(true, "Analysis appears objective - findings distributed across entities")
    }

    /**
     * VO-004: Check temporal accuracy.
     */
    private fun checkTemporalAccuracy(context: ValidationContext): Pair<Boolean, String> {
        if (context.evidenceTimestamps.isEmpty()) {
            return Pair(false, "No timestamps available for validation")
        }

        val now = System.currentTimeMillis()
        val futureTimestamps = context.evidenceTimestamps.filter { it > now + MS_PER_DAY } // 1 day tolerance

        if (futureTimestamps.isNotEmpty()) {
            return Pair(false, "${futureTimestamps.size} timestamp(s) are in the future - validation required")
        }

        // Check for suspiciously old timestamps (before 1990)
        val ancientTimestamps = context.evidenceTimestamps.filter { it < JANUARY_1_1990_MS }
        if (ancientTimestamps.isNotEmpty()) {
            return Pair(false, "${ancientTimestamps.size} timestamp(s) are before 1990 - likely invalid")
        }

        return Pair(true, "All timestamps are within valid range")
    }

    /**
     * VO-005: Check entity protection.
     */
    private fun checkEntityProtection(context: ValidationContext): Pair<Boolean, String> {
        if (!context.privacyMeasuresApplied) {
            return Pair(false, "Privacy measures have not been applied to entity data")
        }

        return Pair(true, "Entity privacy measures are in place")
    }

    /**
     * VO-006: Check disclosure requirements.
     */
    private fun checkDisclosureRequirements(context: ValidationContext): Pair<Boolean, String> {
        if (context.contradictionsFound > context.contradictionsReported) {
            val unreported = context.contradictionsFound - context.contradictionsReported
            return Pair(false, "$unreported contradiction(s) found but not reported - selective omission detected")
        }

        if (context.contradictionsFound < context.contradictionsReported) {
            return Pair(false, "More contradictions reported than found - data integrity issue")
        }

        return Pair(true, "All findings are properly disclosed")
    }

    /**
     * VO-007: Check methodology compliance.
     */
    private fun checkMethodologyCompliance(context: ValidationContext): Pair<Boolean, String> {
        if (!context.methodologyDocumented) {
            return Pair(false, "Analysis methodology is not documented")
        }

        if (context.analysisSteps.isEmpty()) {
            return Pair(false, "No analysis steps recorded")
        }

        return Pair(true, "Methodology is documented with ${context.analysisSteps.size} steps")
    }

    /**
     * VO-008: Check no tampering.
     */
    private fun checkNoTampering(context: ValidationContext): Pair<Boolean, String> {
        if (!context.originalEvidencePreserved) {
            return Pair(false, "CRITICAL: Original evidence has been modified")
        }

        return Pair(true, "Original evidence is preserved")
    }

    /**
     * VO-009: Check contradiction transparency.
     */
    private fun checkContradictionTransparency(context: ValidationContext): Pair<Boolean, String> {
        if (context.contradictionsFound == 0) {
            return Pair(true, "No contradictions to report")
        }

        if (context.contradictionsReported < context.contradictionsFound) {
            val missing = context.contradictionsFound - context.contradictionsReported
            return Pair(false, "$missing contradiction(s) not transparently reported")
        }

        return Pair(true, "All ${context.contradictionsFound} contradiction(s) are reported")
    }

    /**
     * VO-010: Check verifiable analysis.
     */
    private fun checkVerifiableAnalysis(context: ValidationContext): Pair<Boolean, String> {
        if (context.analysisSteps.isEmpty()) {
            return Pair(false, "Analysis steps not recorded - results not verifiable")
        }

        val requiredSteps = listOf("evidence_ingestion", "entity_extraction", "timeline_generation", "contradiction_analysis")
        val missingSteps = requiredSteps.filter { req ->
            context.analysisSteps.none { it.contains(req, ignoreCase = true) }
        }

        if (missingSteps.isNotEmpty()) {
            return Pair(false, "Missing verifiable analysis steps: ${missingSteps.joinToString(", ")}")
        }

        return Pair(true, "Analysis is fully verifiable with ${context.analysisSteps.size} documented steps")
    }

    /**
     * Determine severity based on rule category.
     */
    private fun determineSeverity(category: RuleCategory): AnomalySeverity {
        return when (category) {
            RuleCategory.EVIDENCE_INTEGRITY -> AnomalySeverity.CRITICAL
            RuleCategory.CHAIN_OF_CUSTODY -> AnomalySeverity.HIGH
            RuleCategory.ANALYSIS_OBJECTIVITY -> AnomalySeverity.HIGH
            RuleCategory.TEMPORAL_ACCURACY -> AnomalySeverity.MEDIUM
            RuleCategory.ENTITY_PROTECTION -> AnomalySeverity.HIGH
            RuleCategory.DISCLOSURE_REQUIREMENTS -> AnomalySeverity.CRITICAL
            RuleCategory.METHODOLOGY_COMPLIANCE -> AnomalySeverity.MEDIUM
        }
    }

    /**
     * Generate remediation for a rule violation.
     */
    private fun generateRemediation(rule: Rule): String {
        return when (rule.id) {
            "VO-001" -> "Ensure all evidence is hashed using SHA-512 before analysis"
            "VO-002" -> "Document all evidence handling with timestamps and handler identification"
            "VO-003" -> "Review analysis to ensure all parties are treated equally"
            "VO-004" -> "Validate and correct any invalid timestamps before proceeding"
            "VO-005" -> "Apply appropriate privacy measures to all personal data"
            "VO-006" -> "Ensure all material findings are included in the report"
            "VO-007" -> "Document the analysis methodology according to forensic standards"
            "VO-008" -> "Work only with copies of evidence - never modify originals"
            "VO-009" -> "Report all contradictions regardless of which party they affect"
            "VO-010" -> "Record each analysis step to ensure reproducibility"
            else -> "Review and address the rule violation"
        }
    }

    /**
     * Generate recommendation for a warning.
     */
    private fun generateRecommendation(rule: Rule): String {
        return when (rule.category) {
            RuleCategory.EVIDENCE_INTEGRITY -> "Consider additional integrity verification measures"
            RuleCategory.CHAIN_OF_CUSTODY -> "Strengthen chain of custody documentation"
            RuleCategory.ANALYSIS_OBJECTIVITY -> "Have analysis reviewed by an independent party"
            RuleCategory.TEMPORAL_ACCURACY -> "Cross-reference timestamps with external sources"
            RuleCategory.ENTITY_PROTECTION -> "Review privacy measures for completeness"
            RuleCategory.DISCLOSURE_REQUIREMENTS -> "Double-check that all findings are disclosed"
            RuleCategory.METHODOLOGY_COMPLIANCE -> "Align methodology with industry best practices"
        }
    }

    /**
     * Generate overall recommendations.
     */
    private fun generateRecommendations(
        violations: List<RuleViolation>,
        warnings: List<RuleWarning>,
        context: ValidationContext
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (violations.any { it.ruleId == "VO-001" }) {
            recommendations.add("PRIORITY: Implement SHA-512 hashing for all evidence items")
        }

        if (violations.any { it.ruleId == "VO-008" }) {
            recommendations.add("CRITICAL: Restore original evidence from backup")
        }

        if (context.analysisSteps.size < 4) {
            recommendations.add("Document all analysis steps for verifiability")
        }

        if (context.contradictionsFound > 0 && context.contradictionsReported == 0) {
            recommendations.add("Report all detected contradictions to maintain transparency")
        }

        if (warnings.isNotEmpty()) {
            recommendations.add("Address ${warnings.size} warning(s) to improve compliance")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Analysis complies with Verum Omnis constitutional requirements")
        }

        return recommendations
    }

    /**
     * Calculate compliance score.
     */
    private fun calculateComplianceScore(
        rulesChecked: List<RuleValidation>,
        violations: List<RuleViolation>
    ): Float {
        if (rulesChecked.isEmpty()) return 0f

        // Base score from passed rules
        val passedRatio = rulesChecked.count { it.passed }.toFloat() / rulesChecked.size

        // Deductions for violations by severity
        var deductions = 0f
        for (violation in violations) {
            deductions += when (violation.severity) {
                AnomalySeverity.CRITICAL -> 0.2f
                AnomalySeverity.HIGH -> 0.1f
                AnomalySeverity.MEDIUM -> 0.05f
                AnomalySeverity.LOW -> 0.02f
                AnomalySeverity.INFO -> 0.01f
            }
        }

        return (passedRatio * 100 - deductions * 100).coerceIn(0f, 100f)
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: ConstitutionBrainResult): String {
        return when (result) {
            is ConstitutionBrainResult.Success -> buildSuccessJson(result)
            is ConstitutionBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: ConstitutionBrainResult.Success): String {
        val rulesCheckedJson = result.rulesChecked.joinToString(",") { r ->
            """{"ruleId":"${r.ruleId}","ruleName":"${escapeJson(r.ruleName)}","category":"${r.category}","passed":${r.passed},"details":"${escapeJson(r.details)}"}"""
        }
        val violationsJson = result.violations.joinToString(",") { v ->
            """{"ruleId":"${v.ruleId}","ruleName":"${escapeJson(v.ruleName)}","severity":"${v.severity}","description":"${escapeJson(v.description)}","remediation":"${escapeJson(v.remediation)}"}"""
        }
        val warningsJson = result.warnings.joinToString(",") { w ->
            """{"ruleId":"${w.ruleId}","ruleName":"${escapeJson(w.ruleName)}","description":"${escapeJson(w.description)}","recommendation":"${escapeJson(w.recommendation)}"}"""
        }
        val recommendationsJson = result.recommendations.joinToString(",") { "\"${escapeJson(it)}\"" }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "validationId": "${result.validationId}",
            "compliant": ${result.compliant},
            "rulesChecked": [$rulesCheckedJson],
            "violations": [$violationsJson],
            "warnings": [$warningsJson],
            "recommendations": [$recommendationsJson],
            "complianceScore": ${result.complianceScore}
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: ConstitutionBrainResult.Failure): String {
        return """
        {
            "success": false,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "error": "${escapeJson(result.error)}",
            "errorCode": "${result.errorCode}"
        }
        """.trimIndent()
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
