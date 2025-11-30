package com.verumdec.entity

import com.verumdec.core.model.Statement
import com.verumdec.entity.profile.EntityContradictionDetector
import com.verumdec.entity.profile.EntityProfile

/**
 * Entity Module - Entity Discovery and Claim Extraction
 *
 * This module handles entity discovery and claim extraction from processed documents.
 * It identifies people, organizations, and relationships, and extracts factual claims and statements.
 *
 * ## Key Responsibilities:
 * - Discover entities (names, emails, phone numbers, companies, bank accounts)
 * - Cluster entities by frequency and co-occurrence
 * - Resolve aliases and references ("He", "my partner", "your friend Kevin")
 * - Extract claims and assertions from text
 * - Map statements to entities
 * - Build EntityProfiles for contradiction detection
 *
 * ## Pipeline Stage: 2 - ENTITY DISCOVERY (Who are the players?)
 *
 * ## Entity-Level Contradiction Detection:
 * - Build an EntityProfile for every person, company, site, and legal structure
 * - Track attributes: name, role, actions, claims, dates, financial amounts
 * - Compare EntityProfiles across documents
 * - Flag contradictions when:
 *   - A person denies something they previously admitted
 *   - Document A says X; Document B says NOT X
 *   - A financial figure changes without explanation
 *
 * ## Entity Data Structure:
 * - ID (unique identifier)
 * - Alias list (all names/references)
 * - Unique signatures (email, phone, bank account)
 * - Timeline footprint (where they appear)
 * - Statement map (everything they said)
 * - Behavioral profile (sentiment trends, certainty, patterns)
 *
 * @see com.verumdec.core.CoreModule
 */
object EntityModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "2.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "entity"

    // Singleton detector instance
    private var contradictionDetector: EntityContradictionDetector? = null

    /**
     * Initialize the Entity module.
     * Creates the EntityContradictionDetector.
     */
    fun initialize() {
        contradictionDetector = EntityContradictionDetector()
    }

    /**
     * Get or create the EntityContradictionDetector.
     *
     * @return EntityContradictionDetector instance
     */
    fun getContradictionDetector(): EntityContradictionDetector {
        if (contradictionDetector == null) {
            initialize()
        }
        return contradictionDetector!!
    }

    /**
     * Discover entities from extracted text.
     *
     * @param text Extracted text content
     * @return List of discovered entity names
     */
    fun discoverEntities(text: String): List<String> {
        // Simple entity extraction based on capitalized words and patterns
        val entities = mutableSetOf<String>()
        
        // Find proper nouns (capitalized words not at start of sentence)
        val properNounPattern = Regex("(?<=[.!?]\\s+|^)[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*")
        properNounPattern.findAll(text).forEach { match ->
            if (match.value.length > 2) {
                entities.add(match.value.trim())
            }
        }
        
        // Find email addresses
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        emailPattern.findAll(text).forEach { match ->
            entities.add(match.value)
        }
        
        return entities.toList()
    }

    /**
     * Build entity profiles from statements.
     *
     * @param statements List of statements to process
     * @return Map of entity names to profiles
     */
    fun buildEntityProfiles(statements: List<Statement>): Map<String, EntityProfile> {
        val detector = getContradictionDetector()
        detector.buildProfilesFromStatements(statements)
        return detector.getAllProfiles().associateBy { it.name }
    }

    /**
     * Get an entity profile by name.
     *
     * @param name Entity name to look up
     * @return EntityProfile if found, null otherwise
     */
    fun getEntityProfile(name: String): EntityProfile? {
        return getContradictionDetector().getProfileByName(name)
    }

    /**
     * Detect entity-level contradictions.
     *
     * @return List of detected contradictions
     */
    fun detectContradictions(): List<com.verumdec.core.model.Contradiction> {
        return getContradictionDetector().detectEntityContradictions()
    }

    /**
     * Reset the module state for a new analysis.
     */
    fun reset() {
        contradictionDetector?.clear()
    }
}
