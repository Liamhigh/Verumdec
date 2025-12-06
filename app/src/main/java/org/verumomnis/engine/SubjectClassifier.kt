package org.verumomnis.engine

/**
 * STEP 3: SUBJECT CLASSIFICATION
 * Classify every sentence into zero or more of:
 * - ShareholderOppression
 * - BreachOfFiduciaryDuty
 * - Cybercrime
 * - FraudulentEvidence
 * - EmotionalExploitation
 */
class SubjectClassifier {
    
    private val classificationPatterns = mapOf(
        SubjectTag.SHAREHOLDER_OPPRESSION to listOf(
            "shareholder", "dividend", "equity", "shares", "profit distribution",
            "ownership", "voting rights", "board meeting", "oppression"
        ),
        SubjectTag.BREACH_OF_FIDUCIARY_DUTY to listOf(
            "fiduciary", "duty", "trust", "conflict of interest", "self-dealing",
            "loyalty", "care", "good faith", "director", "trustee"
        ),
        SubjectTag.CYBERCRIME to listOf(
            "hack", "unauthorized access", "password", "breach", "cyber",
            "computer", "data theft", "malware", "phishing", "intrusion"
        ),
        SubjectTag.FRAUDULENT_EVIDENCE to listOf(
            "forged", "fake", "fabricated", "doctored", "altered",
            "manipulated", "falsified", "counterfeit", "tampered"
        ),
        SubjectTag.EMOTIONAL_EXPLOITATION to listOf(
            "manipulate", "gaslight", "pressure", "coerce", "threaten",
            "exploit", "emotional", "abuse", "intimidate", "harass"
        )
    )
    
    /**
     * Classify each sentence into zero or more subject tags.
     * This step ALWAYS runs the same classification logic.
     */
    fun classify(narrativeList: List<Sentence>): List<Sentence> {
        for (sentence in narrativeList) {
            val lowerText = sentence.text.lowercase()
            
            // Check each classification pattern
            for ((tag, keywords) in classificationPatterns) {
                if (keywords.any { keyword -> lowerText.contains(keyword.lowercase()) }) {
                    sentence.subjectTags.add(tag)
                }
            }
        }
        
        return narrativeList
    }
}
