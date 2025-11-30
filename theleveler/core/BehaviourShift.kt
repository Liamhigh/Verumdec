package ai.verum.theleveler.core

/**
 * Detects tone changes, linguistic drift, certainty changes.
 */
data class BehaviourShift(
    val actor: Actor,
    val shiftType: BehaviourShiftType,
    val description: String,
    val fromStatement: Statement,
    val toStatement: Statement
)

enum class BehaviourShiftType {
    CERTAINTY_DROP,
    CERTAINTY_RISE,
    DEFENSIVE_TONE,
    AGGRESSIVE_TONE,
    EVASIVE_TONE,
    LINGUISTIC_DRIFT
}
