package ai.verum.theleveler.core

/**
 * A single extracted statement from text.
 * timestamp is stored as a string for maximum real-world flexibility.
 */
data class Statement(
    val actor: Actor,
    val text: String,
    val timestamp: String? = null,
    val sourceId: String? = null
)
