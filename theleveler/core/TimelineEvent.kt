package ai.verum.theleveler.core

/**
 * Represents a chronological event extracted from text.
 */
data class TimelineEvent(
    val description: String,
    val timestamp: String?,
    val actor: Actor? = null,
    val sourceId: String? = null
)
