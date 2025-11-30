package ai.verum.theleveler.extraction

import ai.verum.theleveler.core.Actor

/**
 * Extracts speaker/actor from text patterns.
 * Handles common formats like "Name:", "[Name]", etc.
 */
object SpeakerExtractor {

    private val speakerPatterns = listOf(
        Regex("^\\[([^\\]]+)\\]:?\\s*"),      // [Name]: or [Name]
        Regex("^([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?):\\s*"),  // Name Name:
        Regex("^([A-Z][a-z]+):\\s*")           // Name:
    )

    fun extractSpeaker(line: String): Pair<Actor?, String> {
        for (pattern in speakerPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                val name = match.groupValues[1].trim()
                val remainder = line.removePrefix(match.value).trim()
                return Actor(name) to remainder
            }
        }
        return null to line
    }
}
