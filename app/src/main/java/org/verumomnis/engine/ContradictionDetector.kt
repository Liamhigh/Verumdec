package org.verumomnis.engine

/**
 * STEP 4: CONTRADICTION DETECTION
 * Compare sentences to each other and to evidence files.
 * If two statements conflict, mark contradiction severity HIGH.
 */
class ContradictionDetector {
    
    /**
     * Detect contradictions between sentences.
     * This step ALWAYS runs the same detection logic.
     */
    fun detectContradictions(narrativeList: List<Sentence>): List<ContradictionResult> {
        val contradictions = mutableListOf<ContradictionResult>()
        
        // Compare each pair of sentences for contradictions
        for (i in narrativeList.indices) {
            for (j in i + 1 until narrativeList.size) {
                val sentenceA = narrativeList[i]
                val sentenceB = narrativeList[j]
                
                val contradiction = checkForContradiction(sentenceA, sentenceB)
                if (contradiction != null) {
                    contradictions.add(contradiction)
                    
                    // Flag both sentences as having contradictions
                    sentenceA.isFlagged = true
                    sentenceB.isFlagged = true
                }
            }
        }
        
        return contradictions
    }
    
    /**
     * Check if two sentences contradict each other.
     * Uses basic negation and opposite meaning detection.
     */
    private fun checkForContradiction(a: Sentence, b: Sentence): ContradictionResult? {
        val textA = a.text.lowercase()
        val textB = b.text.lowercase()
        
        // Detection patterns for contradictions
        val contradictionPairs = listOf(
            "yes" to "no",
            "true" to "false",
            "did" to "did not",
            "will" to "will not",
            "have" to "have not",
            "was" to "was not",
            "is" to "is not",
            "agree" to "disagree",
            "admit" to "deny",
            "confirm" to "refute",
            "accept" to "reject"
        )
        
        for ((positive, negative) in contradictionPairs) {
            if ((textA.contains(positive) && textB.contains(negative)) ||
                (textA.contains(negative) && textB.contains(positive))) {
                return ContradictionResult(
                    statementA = a,
                    statementB = b,
                    description = "Contradictory statements detected: one affirms while the other denies",
                    severity = SeverityLevel.HIGH
                )
            }
        }
        
        return null
    }
}
