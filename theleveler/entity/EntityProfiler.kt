package ai.verum.theleveler.entity

import ai.verum.theleveler.core.*

/**
 * Profiles entities (actors) based on their statements and behaviour.
 * Extracts themes, patterns, and summary statistics.
 */
object EntityProfiler {

    // Theme keywords for extraction
    private val themeKeywords = listOf(
        "payment", "money", "fuel", "delivery", "agreement", 
        "threat", "refund", "promise", "contract", "invoice"
    )

    /**
     * Profile all actors from statements.
     * Returns a list of EntityProfile objects.
     */
    fun profile(statements: List<Statement>): List<EntityProfile> {
        val grouped = statements.groupBy { it.actor.normalized }
        
        return grouped.map { (_, actorStatements) ->
            EntityProfile(
                actor = actorStatements.first().actor,
                statementCount = actorStatements.size,
                themes = extractThemes(actorStatements),
                firstSeen = actorStatements.minOfOrNull { it.timestamp ?: "" }?.ifEmpty { null },
                lastSeen = actorStatements.maxOfOrNull { it.timestamp ?: "" }?.ifEmpty { null }
            )
        }
    }

    /**
     * Profile all actors from statements as a map.
     */
    fun profileActors(statements: List<Statement>): Map<String, EntityProfile> {
        return profile(statements).associateBy { it.actor.normalized }
    }

    /**
     * Extract themes from statements.
     */
    private fun extractThemes(statements: List<Statement>): List<String> {
        val found = mutableSetOf<String>()
        
        for (s in statements) {
            for (k in themeKeywords) {
                if (s.text.contains(k, ignoreCase = true)) {
                    found.add(k)
                }
            }
        }
        
        return found.toList()
    }
}
