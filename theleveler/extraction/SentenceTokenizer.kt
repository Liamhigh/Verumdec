package ai.verum.theleveler.extraction

/**
 * Breaks normalized text into sentences.
 */
object SentenceTokenizer {

    fun tokenize(text: String): List<String> {
        return text
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
