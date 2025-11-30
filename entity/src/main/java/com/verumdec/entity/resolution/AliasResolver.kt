package com.verumdec.entity.resolution

/**
 * AliasResolver - Resolves aliases and references to entities.
 */
class AliasResolver {

    // Common pronouns
    private val malePronouns = setOf("he", "him", "his", "himself")
    private val femalePronouns = setOf("she", "her", "hers", "herself")
    private val neutralPronouns = setOf("they", "them", "their", "theirs", "themselves")

    /**
     * Resolve alias references in text.
     */
    fun resolve(
        text: String,
        knownEntities: List<EntityReference>
    ): AliasResolutionResult {
        val resolutions = mutableListOf<ResolvedAlias>()
        val sentences = text.split(Regex("[.!?\\n]+"))
        
        var lastMentionedMale: EntityReference? = null
        var lastMentionedFemale: EntityReference? = null
        var lastMentionedAny: EntityReference? = null
        
        for (sentence in sentences) {
            val lower = sentence.lowercase()
            
            // Track entity mentions
            for (entity in knownEntities) {
                if (lower.contains(entity.name.lowercase())) {
                    lastMentionedAny = entity
                    if (entity.gender == Gender.MALE) lastMentionedMale = entity
                    if (entity.gender == Gender.FEMALE) lastMentionedFemale = entity
                }
            }
            
            // Resolve pronouns
            for (pronoun in malePronouns) {
                if (Regex("\\b$pronoun\\b").containsMatchIn(lower) && lastMentionedMale != null) {
                    resolutions.add(ResolvedAlias(
                        alias = pronoun,
                        resolvedTo = lastMentionedMale.name,
                        entityId = lastMentionedMale.id,
                        confidence = 0.70f,
                        context = sentence.take(100)
                    ))
                }
            }
            
            for (pronoun in femalePronouns) {
                if (Regex("\\b$pronoun\\b").containsMatchIn(lower) && lastMentionedFemale != null) {
                    resolutions.add(ResolvedAlias(
                        alias = pronoun,
                        resolvedTo = lastMentionedFemale.name,
                        entityId = lastMentionedFemale.id,
                        confidence = 0.70f,
                        context = sentence.take(100)
                    ))
                }
            }
            
            // Check for relational aliases
            val relationalAliases = extractRelationalAliases(lower)
            for ((alias, possibleEntity) in relationalAliases) {
                val matched = knownEntities.find { 
                    it.name.lowercase().contains(possibleEntity) ||
                    possibleEntity.contains(it.name.lowercase())
                }
                
                if (matched != null) {
                    resolutions.add(ResolvedAlias(
                        alias = alias,
                        resolvedTo = matched.name,
                        entityId = matched.id,
                        confidence = 0.60f,
                        context = sentence.take(100)
                    ))
                }
            }
        }
        
        return AliasResolutionResult(
            resolutions = resolutions.distinctBy { "${it.alias}-${it.entityId}" },
            totalProcessed = sentences.size,
            unresolvedAliases = findUnresolvedAliases(text, resolutions)
        )
    }

    /**
     * Build alias map for entities.
     */
    fun buildAliasMap(entities: List<EntityReference>): Map<String, String> {
        val aliasMap = mutableMapOf<String, String>()
        
        for (entity in entities) {
            aliasMap[entity.name.lowercase()] = entity.id
            
            // Add common variations
            val parts = entity.name.split(" ")
            if (parts.size >= 2) {
                aliasMap[parts.first().lowercase()] = entity.id  // First name
                aliasMap[parts.last().lowercase()] = entity.id   // Last name
            }
        }
        
        return aliasMap
    }

    private fun extractRelationalAliases(text: String): List<Pair<String, String>> {
        val aliases = mutableListOf<Pair<String, String>>()
        
        val patterns = listOf(
            Regex("my (?:friend|partner|colleague|boss|wife|husband|brother|sister|mother|father) ([A-Za-z]+)"),
            Regex("([A-Za-z]+)'s (?:friend|partner|colleague|wife|husband)"),
            Regex("the (?:defendant|plaintiff|accused|complainant) ([A-Za-z]+)?")
        )
        
        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                val fullMatch = match.value
                val name = match.groupValues.getOrNull(1) ?: ""
                if (name.isNotBlank()) {
                    aliases.add(fullMatch to name)
                }
            }
        }
        
        return aliases
    }

    private fun findUnresolvedAliases(
        text: String,
        resolved: List<ResolvedAlias>
    ): List<String> {
        val unresolved = mutableListOf<String>()
        val lower = text.lowercase()
        val resolvedAliases = resolved.map { it.alias.lowercase() }.toSet()
        
        val allPronouns = malePronouns + femalePronouns + neutralPronouns
        for (pronoun in allPronouns) {
            if (Regex("\\b$pronoun\\b").containsMatchIn(lower) && pronoun !in resolvedAliases) {
                unresolved.add(pronoun)
            }
        }
        
        return unresolved.distinct()
    }
}

enum class Gender {
    MALE, FEMALE, UNKNOWN
}

data class EntityReference(
    val id: String,
    val name: String,
    val gender: Gender = Gender.UNKNOWN
)

data class ResolvedAlias(
    val alias: String,
    val resolvedTo: String,
    val entityId: String,
    val confidence: Float,
    val context: String
)

data class AliasResolutionResult(
    val resolutions: List<ResolvedAlias>,
    val totalProcessed: Int,
    val unresolvedAliases: List<String>
)
