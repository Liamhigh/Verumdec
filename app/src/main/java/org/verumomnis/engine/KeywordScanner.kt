package org.verumomnis.engine

/**
 * STEP 7: KEYWORD SCAN
 * Search each sentence for:
 * ["admit","deny","forged","delete","access","refuse","invoice","profit"]
 */
class KeywordScanner {
    
    private val targetKeywords = listOf(
        "admit",
        "deny",
        "forged",
        "delete",
        "access",
        "refuse",
        "invoice",
        "profit"
    )
    
    /**
     * Scan for specific keywords in each sentence.
     * This step ALWAYS scans for the SAME keywords.
     */
    fun scanKeywords(narrativeList: List<Sentence>): List<Sentence> {
        for (sentence in narrativeList) {
            val lowerText = sentence.text.lowercase()
            
            // Check for each target keyword
            for (keyword in targetKeywords) {
                if (lowerText.contains(keyword.lowercase())) {
                    sentence.keywords.add(keyword)
                    sentence.isFlagged = true
                }
            }
        }
        
        return narrativeList
    }
}
