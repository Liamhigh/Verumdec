package org.verumomnis.engine

/**
 * STEP 5: OMISSION DETECTION
 * Detect:
 * - Missing context
 * - Hidden details
 * - Deleted messages
 * - Cropped screenshots
 * - Timeline gaps
 * Mark omissions = MEDIUM or HIGH severity.
 */
class OmissionDetector {
    
    private val omissionIndicators = listOf(
        "..." to "Ellipsis indicating missing content",
        "[deleted]" to "Explicitly deleted message",
        "[redacted]" to "Redacted information",
        "cropped" to "Cropped screenshot mentioned",
        "missing" to "Missing information referenced",
        "not provided" to "Evidence not provided",
        "unavailable" to "Information unavailable",
        "can't find" to "Unable to locate evidence",
        "lost" to "Evidence lost",
        "gap in" to "Gap in timeline or evidence"
    )
    
    /**
     * Detect omissions in the narrative.
     * This step ALWAYS runs the same detection logic.
     */
    fun detectOmissions(narrativeList: List<Sentence>): List<OmissionResult> {
        val omissions = mutableListOf<OmissionResult>()
        
        // Scan each sentence for omission indicators
        for (sentence in narrativeList) {
            val lowerText = sentence.text.lowercase()
            
            for ((indicator, description) in omissionIndicators) {
                if (lowerText.contains(indicator.lowercase())) {
                    val severity = when {
                        indicator in listOf("[deleted]", "[redacted]", "lost") -> SeverityLevel.HIGH
                        else -> SeverityLevel.MEDIUM
                    }
                    
                    omissions.add(
                        OmissionResult(
                            description = description,
                            context = sentence.text,
                            severity = severity
                        )
                    )
                    
                    sentence.isFlagged = true
                }
            }
        }
        
        // Check for timeline gaps (missing sequential timestamps)
        val sortedSentences = narrativeList.sortedBy { it.timestamp ?: 0L }
        for (i in 0 until sortedSentences.size - 1) {
            val current = sortedSentences[i].timestamp ?: 0L
            val next = sortedSentences[i + 1].timestamp ?: 0L
            
            // If gap is larger than expected, flag it
            if (next - current > 86400000) { // More than 1 day
                omissions.add(
                    OmissionResult(
                        description = "Timeline gap detected",
                        context = "Gap between evidence items suggests missing information",
                        severity = SeverityLevel.MEDIUM
                    )
                )
            }
        }
        
        return omissions
    }
}
