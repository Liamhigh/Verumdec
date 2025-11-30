package ai.verum.theleveler.core

/**
 * Central output of the contradiction engine.
 */
data class ContradictionReport(
    val contradictions: List<Contradiction>,
    val behaviourShifts: List<BehaviourShift>
)
