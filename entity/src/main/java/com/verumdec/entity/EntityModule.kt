package com.verumdec.entity

/**
 * Entity Module - Entity Discovery and Claim Extraction
 *
 * This module handles entity discovery from processed documents.
 * It identifies people, organizations, and their relationships.
 *
 * ## Pipeline Stage: 2 - ENTITY DISCOVERY (Who are the players?)
 *
 * ## Capabilities:
 * - Named Entity Recognition (names, emails, phones, companies, bank accounts)
 * - Entity clustering by frequency and co-occurrence
 * - Alias and reference resolution ("He", "my partner", etc.)
 * - Claim and assertion extraction
 * - Statement mapping per entity
 *
 * ## Entity Data Structure:
 * - ID (unique identifier)
 * - Alias list (all names/references)
 * - Unique signatures (email, phone, bank account)
 * - Timeline footprint (where they appear)
 * - Statement map (everything they said)
 *
 * @see EntityDiscovery
 * @see ClaimExtractor
 */
object EntityModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "entity"

    /**
     * Initialize the Entity module.
     */
    fun initialize() {
        // Entity module initialization
    }

    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            components = listOf("EntityDiscovery", "ClaimExtractor", "AliasResolver")
        )
    }
    
    data class ModuleInfo(
        val name: String,
        val version: String,
        val components: List<String>
    )
}
