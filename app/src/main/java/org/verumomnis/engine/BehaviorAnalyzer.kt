package org.verumomnis.engine

/**
 * STEP 6: BEHAVIORAL ANALYSIS
 * Scan each sentence for:
 * - Evasion
 * - Gaslighting
 * - Blame-shifting
 * - Selective disclosure
 * - Refusal to answer
 * - Justification loops
 * - Unauthorized access attempts
 * Record behavior with severity.
 */
class BehaviorAnalyzer {
    
    private val behaviorPatterns = mapOf(
        BehaviorFlag.EVASION to listOf(
            "i don't recall", "i'm not sure", "maybe", "perhaps", "possibly",
            "i can't remember", "unclear", "vague"
        ),
        BehaviorFlag.GASLIGHTING to listOf(
            "you're crazy", "you're imagining", "that never happened",
            "you're overreacting", "you're too sensitive", "you misunderstood"
        ),
        BehaviorFlag.BLAME_SHIFTING to listOf(
            "it's your fault", "you made me", "because of you",
            "you caused", "blame yourself", "your responsibility"
        ),
        BehaviorFlag.SELECTIVE_DISCLOSURE to listOf(
            "some details", "partial", "only certain", "selected information",
            "specific parts", "chosen to share"
        ),
        BehaviorFlag.REFUSAL_TO_ANSWER to listOf(
            "no comment", "refuse to answer", "won't discuss",
            "decline to respond", "not answering", "won't say"
        ),
        BehaviorFlag.JUSTIFICATION_LOOPS to listOf(
            "because", "justified", "had to", "no choice",
            "forced to", "explanation is", "reason being"
        ),
        BehaviorFlag.UNAUTHORIZED_ACCESS_ATTEMPTS to listOf(
            "tried to access", "attempted login", "unauthorized",
            "without permission", "broke in", "hacked", "breached"
        )
    )
    
    /**
     * Analyze behavioral patterns in sentences.
     * This step ALWAYS runs the same analysis logic.
     */
    fun analyzeBehavior(narrativeList: List<Sentence>): List<Sentence> {
        for (sentence in narrativeList) {
            val lowerText = sentence.text.lowercase()
            
            // Check each behavioral pattern
            for ((flag, keywords) in behaviorPatterns) {
                if (keywords.any { keyword -> lowerText.contains(keyword.lowercase()) }) {
                    sentence.behaviors.add(flag)
                    sentence.isFlagged = true
                }
            }
        }
        
        return narrativeList
    }
}
