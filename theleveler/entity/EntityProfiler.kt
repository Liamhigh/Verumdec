package ai.verum.theleveler.entity

import ai.verum.theleveler.core.*

/**
 * Profiles entities (actors) based on their statements and behaviour.
 * Extracts themes, patterns, and summary statistics.
 */
object EntityProfiler {

    // Theme keywords for extraction
    private val themeKeywords = mapOf(
        "financial" to listOf("payment", "money", "amount", "invoice", "fee", "cost", "price", "paid", "owed", "debt"),
        "agreement" to listOf("agreement", "contract", "deal", "terms", "signed", "promise", "committed"),
        "communication" to listOf("said", "told", "emailed", "called", "messaged", "texted", "spoke"),
        "timing" to listOf("date", "time", "when", "before", "after", "during", "while"),
        "location" to listOf("where", "place", "location", "address", "home", "office", "work"),
        "dispute" to listOf("dispute", "argument", "conflict", "disagreement", "problem", "issue"),
        "evidence" to listOf("proof", "evidence", "document", "photo", "record", "file", "receipt")
    )

    /**
     * Profile all actors from statements.
     */
    fun profileActors(statements: List<Statement>): Map<String, EntityProfile> {
        val byActor = statements.groupBy { it.actor.normalized }
        
        return byActor.mapValues { (_, actorStatements) ->
            createProfile(actorStatements)
        }
    }

    /**
     * Create a profile for an actor based on their statements.
     */
    private fun createProfile(statements: List<Statement>): EntityProfile {
        if (statements.isEmpty()) {
            return EntityProfile(
                actor = Actor("Unknown"),
                statementCount = 0,
                themes = emptyList(),
                keyPhrases = emptyList(),
                averageCertainty = 0.0,
                communicationStyle = CommunicationStyle.NEUTRAL
            )
        }
        
        val actor = statements.first().actor
        val allText = statements.joinToString(" ") { it.text }
        
        // Extract themes
        val themes = extractThemes(allText)
        
        // Extract key phrases
        val keyPhrases = extractKeyPhrases(statements)
        
        // Calculate average certainty
        val avgCertainty = calculateAverageCertainty(statements)
        
        // Determine communication style
        val style = determineCommunicationStyle(statements)
        
        return EntityProfile(
            actor = actor,
            statementCount = statements.size,
            themes = themes,
            keyPhrases = keyPhrases,
            averageCertainty = avgCertainty,
            communicationStyle = style
        )
    }

    /**
     * Extract themes from text.
     */
    private fun extractThemes(text: String): List<String> {
        val lower = text.lowercase()
        
        return themeKeywords.filter { (_, keywords) ->
            keywords.any { lower.contains(it) }
        }.keys.toList()
    }

    /**
     * Extract key phrases from statements.
     */
    private fun extractKeyPhrases(statements: List<Statement>): List<String> {
        val phrases = mutableListOf<String>()
        
        for (statement in statements) {
            // Extract quoted phrases
            val quotePattern = Regex("\"([^\"]+)\"")
            quotePattern.findAll(statement.text).forEach { 
                phrases.add(it.groupValues[1]) 
            }
            
            // Extract phrases with numbers (amounts, dates)
            val numberPattern = Regex("\\b\\d+(?:[.,]\\d+)?\\s*(?:dollars?|euros?|pounds?|%|percent)?\\b")
            numberPattern.findAll(statement.text).forEach { 
                phrases.add(it.value) 
            }
        }
        
        return phrases.distinct().take(10)
    }

    /**
     * Calculate average certainty across statements.
     */
    private fun calculateAverageCertainty(statements: List<Statement>): Double {
        if (statements.isEmpty()) return 0.5
        
        val certainties = statements.map { 
            ai.verum.theleveler.analysis.Similarity.certaintyLevel(it.text) 
        }
        
        return certainties.average()
    }

    /**
     * Determine overall communication style.
     */
    private fun determineCommunicationStyle(statements: List<Statement>): CommunicationStyle {
        val allText = statements.joinToString(" ") { it.text.lowercase() }
        
        val defensiveScore = listOf("didn't", "wasn't", "never", "not me", "i swear")
            .count { allText.contains(it) }
        
        val aggressiveScore = listOf("you always", "your fault", "blame", "accuse")
            .count { allText.contains(it) }
        
        val evasiveScore = listOf("don't remember", "can't recall", "not sure", "maybe")
            .count { allText.contains(it) }
        
        val cooperativeScore = listOf("i understand", "i agree", "you're right", "fair point")
            .count { allText.contains(it) }
        
        return when {
            defensiveScore >= 3 -> CommunicationStyle.DEFENSIVE
            aggressiveScore >= 2 -> CommunicationStyle.AGGRESSIVE
            evasiveScore >= 3 -> CommunicationStyle.EVASIVE
            cooperativeScore >= 2 -> CommunicationStyle.COOPERATIVE
            else -> CommunicationStyle.NEUTRAL
        }
    }

    /**
     * Entity profile data class.
     */
    data class EntityProfile(
        val actor: Actor,
        val statementCount: Int,
        val themes: List<String>,
        val keyPhrases: List<String>,
        val averageCertainty: Double,
        val communicationStyle: CommunicationStyle
    )

    /**
     * Communication style enum.
     */
    enum class CommunicationStyle {
        NEUTRAL,
        COOPERATIVE,
        DEFENSIVE,
        AGGRESSIVE,
        EVASIVE
    }
}
