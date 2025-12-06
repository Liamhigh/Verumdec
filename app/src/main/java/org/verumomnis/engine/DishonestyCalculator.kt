package org.verumomnis.engine

/**
 * STEP 9: DISHONESTY SCORE
 * dishonesty_score = flagged_sentences / total_sentences * 100
 */
class DishonestyCalculator {
    
    /**
     * Calculate dishonesty score as percentage of flagged sentences.
     * This step ALWAYS uses the same calculation formula.
     */
    fun calculateDishonestyScore(narrativeList: List<Sentence>): Float {
        if (narrativeList.isEmpty()) {
            return 0f
        }
        
        val totalSentences = narrativeList.size
        val flaggedSentences = narrativeList.count { it.isFlagged }
        
        return (flaggedSentences.toFloat() / totalSentences.toFloat()) * 100f
    }
}
