package ai.verum.theleveler.core

/**
 * Represents a person or entity making statements.
 * Normalised name ensures consistent contradiction detection.
 */
data class Actor(
    val rawName: String,
    val normalized: String = rawName.lowercase().trim()
) {
    /**
     * Display name for use in narratives and reports.
     */
    val displayName: String
        get() = rawName.split(" ").joinToString(" ") { 
            it.replaceFirstChar { c -> c.uppercaseChar() } 
        }
}
