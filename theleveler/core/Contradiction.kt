package ai.verum.theleveler.core

/**
 * A contradiction between two statements or events.
 */
data class Contradiction(
    val actor: Actor,
    val firstStatement: Statement,
    val secondStatement: Statement,
    val type: ContradictionType,
    val confidence: Double,
    val explanation: String
)

enum class ContradictionType {
    DIRECT_CONTRADICTION,
    IMPLICIT_CONTRADICTION,
    TIMELINE_CONTRADICTION,
    FACTUAL_INCONSISTENCY,
    BEHAVIOURAL_SHIFT
}
