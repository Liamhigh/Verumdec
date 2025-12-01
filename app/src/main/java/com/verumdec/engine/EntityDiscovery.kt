package com.verumdec.engine

import com.verumdec.data.*
import java.util.*

/**
 * Entity Discovery System
 * Automatically discovers entities (people, companies, etc.) from evidence text.
 */
class EntityDiscovery {

    // Patterns for entity extraction
    private val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val phonePattern = Regex("\\+?[0-9][0-9\\s\\-()]{8,}[0-9]")
    private val bankAccountPattern = Regex("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}\\b|\\b[0-9]{8,20}\\b")
    
    // Common name patterns (simple approach)
    private val namePattern = Regex("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+\\b")
    
    // Pronouns and references to track
    private val pronouns = setOf("he", "she", "they", "him", "her", "them", "his", "hers", "their")
    private val references = setOf("my partner", "my friend", "the client", "the buyer", "the seller")

    /**
     * Discover all entities from a list of processed evidence.
     */
    fun discoverEntities(evidenceList: List<Evidence>): List<Entity> {
        val entityMap = mutableMapOf<String, MutableEntityBuilder>()

        for (evidence in evidenceList) {
            if (evidence.extractedText.isEmpty()) continue
            
            val text = evidence.extractedText
            
            // Extract emails
            emailPattern.findAll(text).forEach { match ->
                val email = match.value.lowercase()
                val entityName = email.substringBefore('@')
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                
                val builder = entityMap.getOrPut(entityName.lowercase()) {
                    MutableEntityBuilder(primaryName = entityName)
                }
                if (email !in builder.emails) {
                    builder.emails.add(email)
                }
                builder.mentions++
            }
            
            // Extract phone numbers
            phonePattern.findAll(text).forEach { match ->
                val phone = match.value.replace(Regex("[\\s\\-()]"), "")
                if (phone.length >= 10) {
                    // Try to associate with nearby entity or create new
                    val nearbyName = findNearbyName(text, match.range.first)
                    if (nearbyName != null) {
                        val builder = entityMap.getOrPut(nearbyName.lowercase()) {
                            MutableEntityBuilder(primaryName = nearbyName)
                        }
                        if (phone !in builder.phones) {
                            builder.phones.add(phone)
                        }
                    }
                }
            }
            
            // Extract names
            namePattern.findAll(text).forEach { match ->
                val name = match.value
                // Filter out common false positives
                if (!isCommonPhrase(name)) {
                    val builder = entityMap.getOrPut(name.lowercase()) {
                        MutableEntityBuilder(primaryName = name)
                    }
                    builder.mentions++
                }
            }
            
            // Extract from email metadata
            evidence.metadata.sender?.let { sender ->
                val name = extractNameFromEmailAddress(sender)
                if (name != null) {
                    val builder = entityMap.getOrPut(name.lowercase()) {
                        MutableEntityBuilder(primaryName = name)
                    }
                    val email = emailPattern.find(sender)?.value?.lowercase()
                    if (email != null && email !in builder.emails) {
                        builder.emails.add(email)
                    }
                    builder.mentions++
                }
            }
            
            evidence.metadata.receiver?.let { receiver ->
                val name = extractNameFromEmailAddress(receiver)
                if (name != null) {
                    val builder = entityMap.getOrPut(name.lowercase()) {
                        MutableEntityBuilder(primaryName = name)
                    }
                    val email = emailPattern.find(receiver)?.value?.lowercase()
                    if (email != null && email !in builder.emails) {
                        builder.emails.add(email)
                    }
                    builder.mentions++
                }
            }
        }

        // Merge similar entities
        val mergedEntities = mergeEntities(entityMap.values.toList())
        
        // Filter to only entities with significant presence (mentioned in multiple places)
        return mergedEntities
            .filter { it.mentions >= 2 }
            .sortedByDescending { it.mentions }
            .map { it.toEntity() }
    }

    /**
     * Find a name near a position in text (for associating phone numbers).
     */
    private fun findNearbyName(text: String, position: Int): String? {
        val start = maxOf(0, position - 50)
        val end = minOf(text.length, position + 50)
        val window = text.substring(start, end)
        
        return namePattern.find(window)?.value
    }

    /**
     * Check if a phrase is a common expression rather than a name.
     */
    private fun isCommonPhrase(text: String): Boolean {
        val commonPhrases = setOf(
            "The Company", "The Bank", "The Court", "Dear Sir", "Kind Regards",
            "Best Regards", "Yours Sincerely", "Yours Faithfully", "The Agreement",
            "The Contract", "The Deal", "The Money", "The Payment", "Good Morning",
            "Good Afternoon", "Good Evening"
        )
        return text in commonPhrases
    }

    /**
     * Extract a readable name from an email address.
     */
    private fun extractNameFromEmailAddress(emailStr: String): String? {
        // Try to get name from format "Name <email@example.com>"
        val nameMatch = Regex("([^<]+)<").find(emailStr)
        if (nameMatch != null) {
            return nameMatch.groupValues[1].trim()
        }
        
        // Otherwise extract from email username
        val email = emailPattern.find(emailStr)?.value ?: return null
        val username = email.substringBefore('@')
        
        return username
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .takeIf { it.isNotBlank() }
    }

    /**
     * Merge entities that appear to be the same person.
     */
    private fun mergeEntities(entities: List<MutableEntityBuilder>): List<MutableEntityBuilder> {
        val merged = mutableListOf<MutableEntityBuilder>()
        val used = mutableSetOf<Int>()

        for (i in entities.indices) {
            if (i in used) continue
            
            val current = entities[i].copy()
            
            for (j in (i + 1) until entities.size) {
                if (j in used) continue
                
                val other = entities[j]
                if (shouldMerge(current, other)) {
                    // Merge other into current
                    current.aliases.add(other.primaryName)
                    current.aliases.addAll(other.aliases)
                    current.emails.addAll(other.emails)
                    current.phones.addAll(other.phones)
                    current.mentions += other.mentions
                    used.add(j)
                }
            }
            
            merged.add(current)
        }
        
        return merged
    }

    /**
     * Determine if two entities should be merged.
     */
    private fun shouldMerge(a: MutableEntityBuilder, b: MutableEntityBuilder): Boolean {
        // Same email
        if (a.emails.any { it in b.emails }) return true
        
        // Same phone
        if (a.phones.any { it in b.phones }) return true
        
        // Name is substring of other (e.g., "John" and "John Smith")
        val aName = a.primaryName.lowercase()
        val bName = b.primaryName.lowercase()
        if (aName.contains(bName) || bName.contains(aName)) return true
        
        // First name match
        val aFirst = aName.split(" ").firstOrNull()
        val bFirst = bName.split(" ").firstOrNull()
        if (aFirst != null && aFirst == bFirst && aFirst.length > 2) {
            // Additional check - similar structure
            if (a.emails.isNotEmpty() || b.emails.isNotEmpty()) return true
        }
        
        return false
    }

    /**
     * Mutable builder for entity construction.
     */
    private data class MutableEntityBuilder(
        val primaryName: String,
        val aliases: MutableList<String> = mutableListOf(),
        val emails: MutableList<String> = mutableListOf(),
        val phones: MutableList<String> = mutableListOf(),
        var mentions: Int = 0
    ) {
        fun toEntity(): Entity {
            return Entity(
                primaryName = primaryName,
                aliases = aliases.distinct().toMutableList(),
                emails = emails.distinct().toMutableList(),
                phones = phones.distinct().toMutableList(),
                mentions = mentions
            )
        }
        
        fun copy(): MutableEntityBuilder {
            return MutableEntityBuilder(
                primaryName = primaryName,
                aliases = aliases.toMutableList(),
                emails = emails.toMutableList(),
                phones = phones.toMutableList(),
                mentions = mentions
            )
        }
    }
}
