package com.verumdec.entity.discovery

import java.util.*

/**
 * EntityDiscoverer - Discovers entities from text.
 */
class EntityDiscoverer {

    // Email pattern
    private val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    
    // Phone pattern (international format)
    private val phonePattern = Regex("\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{1,3}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}")
    
    // Name pattern (basic capitalized words)
    private val namePattern = Regex("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+\\b")
    
    // Common titles to filter
    private val titles = setOf("Mr", "Mrs", "Ms", "Dr", "Prof", "Sir", "Madam")

    /**
     * Discover all entities in text.
     */
    fun discover(text: String): DiscoveryResult {
        val emails = discoverEmails(text)
        val phones = discoverPhones(text)
        val names = discoverNames(text)
        val organizations = discoverOrganizations(text)

        val entities = mutableListOf<DiscoveredEntity>()
        var entityIndex = 1

        // Create entity for each email
        for (email in emails) {
            entities.add(DiscoveredEntity(
                id = "entity-$entityIndex",
                type = EntityType.PERSON,
                primaryIdentifier = email,
                identifiers = listOf(email),
                mentions = countOccurrences(text, email),
                confidence = 0.95f
            ))
            entityIndex++
        }

        // Create entity for each unique name
        for (name in names) {
            if (!entities.any { it.primaryIdentifier.equals(name, ignoreCase = true) }) {
                entities.add(DiscoveredEntity(
                    id = "entity-$entityIndex",
                    type = EntityType.PERSON,
                    primaryIdentifier = name,
                    identifiers = listOf(name),
                    mentions = countOccurrences(text, name),
                    confidence = 0.75f
                ))
                entityIndex++
            }
        }

        // Create entity for each organization
        for (org in organizations) {
            entities.add(DiscoveredEntity(
                id = "entity-$entityIndex",
                type = EntityType.ORGANIZATION,
                primaryIdentifier = org,
                identifiers = listOf(org),
                mentions = countOccurrences(text, org),
                confidence = 0.70f
            ))
            entityIndex++
        }

        return DiscoveryResult(
            entities = entities,
            emails = emails,
            phones = phones,
            names = names,
            organizations = organizations,
            discoveredAt = Date()
        )
    }

    /**
     * Merge entities that are likely the same person.
     */
    fun mergeEntities(entities: List<DiscoveredEntity>): List<DiscoveredEntity> {
        val merged = mutableListOf<DiscoveredEntity>()
        val used = mutableSetOf<String>()

        for (entity in entities.sortedByDescending { it.mentions }) {
            if (entity.id in used) continue

            val similar = entities.filter { other ->
                other.id != entity.id && 
                other.id !in used &&
                areSimilar(entity, other)
            }

            if (similar.isEmpty()) {
                merged.add(entity)
            } else {
                val allIdentifiers = (listOf(entity) + similar)
                    .flatMap { it.identifiers }
                    .distinct()
                val totalMentions = (listOf(entity) + similar).sumOf { it.mentions }

                merged.add(entity.copy(
                    identifiers = allIdentifiers,
                    mentions = totalMentions
                ))
                used.addAll(similar.map { it.id })
            }
            used.add(entity.id)
        }

        return merged
    }

    private fun discoverEmails(text: String): List<String> {
        return emailPattern.findAll(text).map { it.value.lowercase() }.distinct().toList()
    }

    private fun discoverPhones(text: String): List<String> {
        return phonePattern.findAll(text)
            .map { it.value.replace(Regex("[^0-9+]"), "") }
            .filter { it.length >= 10 }
            .distinct()
            .toList()
    }

    private fun discoverNames(text: String): List<String> {
        return namePattern.findAll(text)
            .map { it.value }
            .filter { name -> !titles.any { name.startsWith(it) } }
            .groupBy { it }
            .filter { it.value.size >= 1 }
            .keys
            .toList()
    }

    private fun discoverOrganizations(text: String): List<String> {
        val orgIndicators = listOf("Ltd", "LLC", "Inc", "Corp", "Company", "Bank", "Trust", "Group")
        val orgs = mutableListOf<String>()

        for (indicator in orgIndicators) {
            val pattern = Regex("\\b[A-Z][\\w\\s]+$indicator\\b")
            orgs.addAll(pattern.findAll(text).map { it.value })
        }

        return orgs.distinct()
    }

    private fun countOccurrences(text: String, term: String): Int {
        return text.split(Regex("(?i)${Regex.escape(term)}")).size - 1
    }

    private fun areSimilar(a: DiscoveredEntity, b: DiscoveredEntity): Boolean {
        // Same type
        if (a.type != b.type) return false

        // Check if any identifiers match
        for (idA in a.identifiers) {
            for (idB in b.identifiers) {
                if (idA.equals(idB, ignoreCase = true)) return true
                if (idA.contains(idB, ignoreCase = true) || idB.contains(idA, ignoreCase = true)) return true
            }
        }

        return false
    }
}

enum class EntityType {
    PERSON, ORGANIZATION, UNKNOWN
}

data class DiscoveredEntity(
    val id: String,
    val type: EntityType,
    val primaryIdentifier: String,
    val identifiers: List<String>,
    val mentions: Int,
    val confidence: Float
)

data class DiscoveryResult(
    val entities: List<DiscoveredEntity>,
    val emails: List<String>,
    val phones: List<String>,
    val names: List<String>,
    val organizations: List<String>,
    val discoveredAt: Date
)
