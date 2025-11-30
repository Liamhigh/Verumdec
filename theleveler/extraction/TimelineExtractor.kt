package ai.verum.theleveler.extraction

import ai.verum.theleveler.core.TimelineEvent

/**
 * Extracts timeline events from text.
 * Looks for date/time patterns and associated descriptions.
 */
object TimelineExtractor {

    private val datePatterns = listOf(
        Regex("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})"),           // DD/MM/YYYY or MM-DD-YY
        Regex("(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})"),             // YYYY-MM-DD
        Regex("([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"),          // Month DD, YYYY
        Regex("(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})")             // DD Month YYYY
    )

    fun extract(text: String): List<TimelineEvent> {
        val normalized = TextNormalizer.normalize(text)
        val sentences = SentenceTokenizer.tokenize(normalized)
        val events = mutableListOf<TimelineEvent>()
        
        for (sentence in sentences) {
            for (pattern in datePatterns) {
                val match = pattern.find(sentence)
                if (match != null) {
                    events.add(
                        TimelineEvent(
                            description = sentence,
                            timestamp = match.value
                        )
                    )
                    break
                }
            }
        }
        
        return events
    }
}
