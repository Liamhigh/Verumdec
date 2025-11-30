package ai.verum.theleveler.extraction

/**
 * Removes noise, standardises punctuation, spacing, and formatting.
 */
object TextNormalizer {

    fun normalize(input: String): String {
        return input
            .replace("\r", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
