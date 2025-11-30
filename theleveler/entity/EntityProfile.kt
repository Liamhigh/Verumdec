package ai.verum.theleveler.entity

import ai.verum.theleveler.core.Actor

/**
 * Profile data for an entity/actor.
 * Contains summary statistics and extracted themes.
 */
data class EntityProfile(
    val actor: Actor,
    val statementCount: Int,
    val themes: List<String>,
    val firstSeen: String?,
    val lastSeen: String?
)
