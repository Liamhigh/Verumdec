package org.verumomnis.engine

/**
 * STEP 8: SEVERITY SCORING
 * Assign:
 * - Low = 1
 * - Medium = 2
 * - High = 3
 * Aggregate per category.
 */
class SeverityScorer {
    
    /**
     * Score severity for each sentence and aggregate by category.
     * This step ALWAYS uses the same scoring logic.
     */
    fun scoreSeverity(narrativeList: List<Sentence>): Map<SubjectTag, Int> {
        val categoryScores = mutableMapOf<SubjectTag, Int>()
        
        // Score each sentence based on flags
        for (sentence in narrativeList) {
            // Determine severity based on flags
            val severity = when {
                sentence.behaviors.isNotEmpty() -> SeverityLevel.HIGH
                sentence.keywords.isNotEmpty() -> SeverityLevel.MEDIUM
                sentence.subjectTags.isNotEmpty() -> SeverityLevel.MEDIUM
                sentence.isFlagged -> SeverityLevel.LOW
                else -> SeverityLevel.LOW
            }
            
            sentence.severity = severity
            
            // Aggregate scores by subject tag
            for (tag in sentence.subjectTags) {
                categoryScores[tag] = (categoryScores[tag] ?: 0) + severity.score
            }
        }
        
        return categoryScores
    }
}
