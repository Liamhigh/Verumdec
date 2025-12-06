package org.verumomnis.engine

/**
 * STEP 10: TOP 3 LIABILITIES
 * Rank by:
 * 1. Total severity
 * 2. Total contradictions
 * 3. Recurrence
 */
class LiabilityExtractor {
    
    /**
     * Extract top 3 liabilities based on severity, contradictions, and recurrence.
     * This step ALWAYS uses the same ranking logic.
     */
    fun extractTopLiabilities(
        categoryScores: Map<SubjectTag, Int>,
        contradictions: List<ContradictionResult>,
        narrativeList: List<Sentence>
    ): List<LiabilityEntry> {
        
        // Count contradictions per category
        val contradictionCounts = mutableMapOf<SubjectTag, Int>()
        for (contradiction in contradictions) {
            for (tag in contradiction.statementA.subjectTags) {
                contradictionCounts[tag] = (contradictionCounts[tag] ?: 0) + 1
            }
        }
        
        // Count recurrence (how many times each category appears)
        val recurrenceCounts = mutableMapOf<SubjectTag, Int>()
        for (sentence in narrativeList) {
            for (tag in sentence.subjectTags) {
                recurrenceCounts[tag] = (recurrenceCounts[tag] ?: 0) + 1
            }
        }
        
        // Build liability entries
        val liabilities = mutableListOf<LiabilityEntry>()
        for (tag in SubjectTag.values()) {
            liabilities.add(
                LiabilityEntry(
                    category = tag,
                    totalSeverity = categoryScores[tag] ?: 0,
                    contradictionCount = contradictionCounts[tag] ?: 0,
                    recurrence = recurrenceCounts[tag] ?: 0
                )
            )
        }
        
        // Sort by:
        // 1. Total severity (primary)
        // 2. Contradiction count (secondary)
        // 3. Recurrence (tertiary)
        val sorted = liabilities.sortedWith(
            compareByDescending<LiabilityEntry> { it.totalSeverity }
                .thenByDescending { it.contradictionCount }
                .thenByDescending { it.recurrence }
        )
        
        // Return top 3
        return sorted.take(3)
    }
}
