package com.verumdec.engine

import com.verumdec.data.*

/**
 * Verum Omnis Constitution Enforcer
 * 
 * Enforces the constitutional principles of the Verum Omnis system at every analysis step.
 * The Constitution ensures:
 * 1. Truth and accuracy in all analysis
 * 2. Protection of privacy and data security
 * 3. Fairness and objectivity in contradiction detection
 * 4. Transparency in liability assessment
 * 5. Proper documentation and sealing of reports
 * 
 * Every analysis operation must pass through constitution validation.
 */
object ConstitutionEnforcer {
    
    /**
     * Constitutional principles enumeration
     */
    enum class Principle {
        TRUTH_ACCURACY,           // All findings must be based on actual evidence
        PRIVACY_PROTECTION,       // All data remains on-device, never transmitted
        FAIRNESS_OBJECTIVITY,     // Analysis must be unbiased and evidence-based
        TRANSPARENCY,             // All scoring and decisions must be explainable
        DOCUMENTATION_INTEGRITY,  // Reports must be complete and sealed
        EVIDENCE_PRESERVATION,    // Original evidence must not be modified
        CONTRADICTION_RIGOR,      // Contradictions must be verifiable
        LIABILITY_JUSTIFICATION,  // Liability scores must have clear basis
        TIMELINE_ACCURACY,        // Timeline events must have verifiable dates
        BEHAVIORAL_EVIDENCE       // Behavioral patterns must have supporting instances
    }
    
    /**
     * Validation result from constitution check
     */
    data class ValidationResult(
        val isValid: Boolean,
        val violations: List<ConstitutionViolation> = emptyList(),
        val warnings: List<String> = emptyList()
    )
    
    /**
     * A constitutional violation
     */
    data class ConstitutionViolation(
        val principle: Principle,
        val description: String,
        val severity: ViolationSeverity,
        val remediation: String
    )
    
    enum class ViolationSeverity {
        CRITICAL,  // Must be fixed before proceeding
        WARNING,   // Should be addressed but doesn't block
        INFO       // Informational only
    }
    
    /**
     * Validate evidence before processing.
     * Ensures evidence integrity and privacy principles.
     */
    fun validateEvidence(evidence: Evidence): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        // Check evidence has required fields
        if (evidence.fileName.isBlank()) {
            violations.add(ConstitutionViolation(
                principle = Principle.DOCUMENTATION_INTEGRITY,
                description = "Evidence must have a valid file name",
                severity = ViolationSeverity.CRITICAL,
                remediation = "Provide a file name for the evidence"
            ))
        }
        
        if (evidence.filePath.isBlank()) {
            violations.add(ConstitutionViolation(
                principle = Principle.EVIDENCE_PRESERVATION,
                description = "Evidence must have a valid file path",
                severity = ViolationSeverity.CRITICAL,
                remediation = "Ensure the evidence file exists and is accessible"
            ))
        }
        
        // Verify privacy - check for no external URLs
        if (evidence.filePath.startsWith("http://") || evidence.filePath.startsWith("https://")) {
            violations.add(ConstitutionViolation(
                principle = Principle.PRIVACY_PROTECTION,
                description = "Evidence must be stored locally, not fetched from external sources",
                severity = ViolationSeverity.CRITICAL,
                remediation = "Download and store evidence locally before analysis"
            ))
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Validate entity discovery results.
     * Ensures fairness and transparency in entity identification.
     */
    fun validateEntities(entities: List<Entity>, evidenceList: List<Evidence>): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        for (entity in entities) {
            // Entities must have a primary name
            if (entity.primaryName.isBlank()) {
                violations.add(ConstitutionViolation(
                    principle = Principle.FAIRNESS_OBJECTIVITY,
                    description = "Entity discovered without a primary identifier",
                    severity = ViolationSeverity.WARNING,
                    remediation = "Review entity discovery to ensure all entities are properly named"
                ))
            }
            
            // Entities should have evidence of their existence
            if (entity.mentions <= 0) {
                warnings.add("Entity '${entity.primaryName}' has no recorded mentions")
            }
        }
        
        // Check for duplicate entities that should be merged
        val names = entities.map { it.primaryName.lowercase() }
        val duplicates = names.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            warnings.add("Potential duplicate entities detected: ${duplicates.joinToString()}")
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Validate timeline events.
     * Ensures timeline accuracy and evidence basis.
     */
    fun validateTimeline(timeline: List<TimelineEvent>, evidenceList: List<Evidence>): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        val evidenceIds = evidenceList.map { it.id }.toSet()
        
        for (event in timeline) {
            // Events must reference valid evidence
            if (event.sourceEvidenceId !in evidenceIds) {
                violations.add(ConstitutionViolation(
                    principle = Principle.TIMELINE_ACCURACY,
                    description = "Timeline event references non-existent evidence: ${event.description.take(50)}",
                    severity = ViolationSeverity.WARNING,
                    remediation = "Verify evidence source for all timeline events"
                ))
            }
            
            // Events must have descriptions
            if (event.description.isBlank()) {
                warnings.add("Timeline event on ${event.date} has no description")
            }
        }
        
        // Check for chronological consistency
        val sortedEvents = timeline.sortedBy { it.date }
        if (sortedEvents != timeline.sortedBy { it.date }) {
            warnings.add("Timeline contains unsorted events")
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Validate contradictions.
     * Ensures contradiction rigor and evidence basis.
     */
    fun validateContradictions(
        contradictions: List<Contradiction>,
        entities: List<Entity>
    ): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        val entityIds = entities.map { it.id }.toSet()
        
        for (contradiction in contradictions) {
            // Contradictions must reference valid entities
            if (contradiction.entityId !in entityIds) {
                violations.add(ConstitutionViolation(
                    principle = Principle.CONTRADICTION_RIGOR,
                    description = "Contradiction references non-existent entity",
                    severity = ViolationSeverity.WARNING,
                    remediation = "Verify entity references in contradiction detection"
                ))
            }
            
            // Contradictions must have descriptions
            if (contradiction.description.isBlank()) {
                violations.add(ConstitutionViolation(
                    principle = Principle.TRANSPARENCY,
                    description = "Contradiction detected without explanation",
                    severity = ViolationSeverity.CRITICAL,
                    remediation = "All contradictions must have clear descriptions"
                ))
            }
            
            // Contradictions should have legal implications
            if (contradiction.legalImplication.isBlank()) {
                warnings.add("Contradiction lacks legal implication explanation")
            }
            
            // Verify statements are distinct
            if (contradiction.statementA.id == contradiction.statementB.id) {
                warnings.add("Contradiction compares statement to itself")
            }
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Validate behavioral patterns.
     * Ensures behavioral evidence requirement is met.
     */
    fun validateBehavioralPatterns(
        patterns: List<BehavioralPattern>,
        entities: List<Entity>
    ): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        val entityIds = entities.map { it.id }.toSet()
        
        for (pattern in patterns) {
            // Patterns must reference valid entities
            if (pattern.entityId !in entityIds) {
                violations.add(ConstitutionViolation(
                    principle = Principle.BEHAVIORAL_EVIDENCE,
                    description = "Behavioral pattern references non-existent entity",
                    severity = ViolationSeverity.WARNING,
                    remediation = "Verify entity references in behavioral analysis"
                ))
            }
            
            // Patterns must have supporting instances
            if (pattern.instances.isEmpty()) {
                violations.add(ConstitutionViolation(
                    principle = Principle.BEHAVIORAL_EVIDENCE,
                    description = "Behavioral pattern '${pattern.type}' detected without supporting evidence",
                    severity = ViolationSeverity.CRITICAL,
                    remediation = "All behavioral patterns must have at least one supporting instance"
                ))
            }
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Validate liability scores.
     * Ensures liability justification requirement is met.
     */
    fun validateLiabilityScores(
        liabilityScores: Map<String, LiabilityScore>,
        entities: List<Entity>,
        contradictions: List<Contradiction>,
        patterns: List<BehavioralPattern>
    ): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        val entityIds = entities.map { it.id }.toSet()
        
        for ((entityId, score) in liabilityScores) {
            // Scores must reference valid entities
            if (entityId !in entityIds) {
                violations.add(ConstitutionViolation(
                    principle = Principle.LIABILITY_JUSTIFICATION,
                    description = "Liability score references non-existent entity",
                    severity = ViolationSeverity.WARNING,
                    remediation = "Verify entity references in liability calculation"
                ))
            }
            
            // Score must be in valid range
            if (score.overallScore < 0f || score.overallScore > 100f) {
                violations.add(ConstitutionViolation(
                    principle = Principle.LIABILITY_JUSTIFICATION,
                    description = "Liability score out of valid range (0-100): ${score.overallScore}",
                    severity = ViolationSeverity.CRITICAL,
                    remediation = "Ensure liability scores are normalized to 0-100 range"
                ))
            }
            
            // High scores should have supporting contradictions or patterns
            if (score.overallScore >= 70f) {
                val entityContradictions = contradictions.count { it.entityId == entityId }
                val entityPatterns = patterns.count { it.entityId == entityId }
                
                if (entityContradictions == 0 && entityPatterns == 0) {
                    warnings.add("High liability score (${score.overallScore}) for entity without contradictions or patterns")
                }
            }
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Validate complete case before report generation.
     * Comprehensive check of all constitutional requirements.
     */
    fun validateCase(case: Case): ValidationResult {
        val allViolations = mutableListOf<ConstitutionViolation>()
        val allWarnings = mutableListOf<String>()
        
        // Validate all evidence
        for (evidence in case.evidence) {
            val result = validateEvidence(evidence)
            allViolations.addAll(result.violations)
            allWarnings.addAll(result.warnings)
        }
        
        // Validate entities
        val entityResult = validateEntities(case.entities, case.evidence)
        allViolations.addAll(entityResult.violations)
        allWarnings.addAll(entityResult.warnings)
        
        // Validate timeline
        val timelineResult = validateTimeline(case.timeline, case.evidence)
        allViolations.addAll(timelineResult.violations)
        allWarnings.addAll(timelineResult.warnings)
        
        // Validate contradictions
        val contradictionResult = validateContradictions(case.contradictions, case.entities)
        allViolations.addAll(contradictionResult.violations)
        allWarnings.addAll(contradictionResult.warnings)
        
        // Case must have a name
        if (case.name.isBlank()) {
            allViolations.add(ConstitutionViolation(
                principle = Principle.DOCUMENTATION_INTEGRITY,
                description = "Case must have a name",
                severity = ViolationSeverity.CRITICAL,
                remediation = "Provide a case name before generating report"
            ))
        }
        
        return ValidationResult(
            isValid = allViolations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = allViolations,
            warnings = allWarnings
        )
    }
    
    /**
     * Validate report before sealing.
     * Final constitutional check before PDF generation.
     */
    fun validateReportForSealing(report: ForensicReport): ValidationResult {
        val violations = mutableListOf<ConstitutionViolation>()
        val warnings = mutableListOf<String>()
        
        // Report must have a case name
        if (report.caseName.isBlank()) {
            violations.add(ConstitutionViolation(
                principle = Principle.DOCUMENTATION_INTEGRITY,
                description = "Report must have a case name",
                severity = ViolationSeverity.CRITICAL,
                remediation = "Provide a case name for the report"
            ))
        }
        
        // Report must have a valid hash
        if (report.sha512Hash.length != 128) {
            violations.add(ConstitutionViolation(
                principle = Principle.DOCUMENTATION_INTEGRITY,
                description = "Report hash is invalid (expected 128 hex characters)",
                severity = ViolationSeverity.CRITICAL,
                remediation = "Regenerate report hash using SHA-512"
            ))
        }
        
        // Report should have narrative
        if (report.narrativeSections.finalSummary.isBlank()) {
            warnings.add("Report has no final summary narrative")
        }
        
        return ValidationResult(
            isValid = violations.none { it.severity == ViolationSeverity.CRITICAL },
            violations = violations,
            warnings = warnings
        )
    }
    
    /**
     * Format validation result for display.
     */
    fun formatValidationResult(result: ValidationResult): String {
        val builder = StringBuilder()
        
        if (result.isValid) {
            builder.appendLine("✅ Constitutional validation passed")
        } else {
            builder.appendLine("❌ Constitutional validation failed")
        }
        
        if (result.violations.isNotEmpty()) {
            builder.appendLine("\nViolations:")
            for (violation in result.violations) {
                val icon = when (violation.severity) {
                    ViolationSeverity.CRITICAL -> "🔴"
                    ViolationSeverity.WARNING -> "🟡"
                    ViolationSeverity.INFO -> "🔵"
                }
                builder.appendLine("$icon [${violation.principle}] ${violation.description}")
                builder.appendLine("   Remediation: ${violation.remediation}")
            }
        }
        
        if (result.warnings.isNotEmpty()) {
            builder.appendLine("\nWarnings:")
            for (warning in result.warnings) {
                builder.appendLine("⚠️ $warning")
            }
        }
        
        return builder.toString()
    }
}
