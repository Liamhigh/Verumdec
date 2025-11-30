package com.verumdec.entity

import android.content.Context
import com.verumdec.entity.discovery.EntityDiscoverer
import com.verumdec.entity.extraction.ClaimExtractor
import com.verumdec.entity.resolution.AliasResolver

/**
 * Entity Module - Entity Discovery and Management
 *
 * This module handles entity discovery and claim extraction from processed documents.
 * It identifies people, organizations, and relationships, and extracts factual claims.
 *
 * ## Key Features:
 * - Discover entities (names, emails, phone numbers, companies)
 * - Cluster entities by frequency and co-occurrence
 * - Resolve aliases and references
 * - Extract claims and assertions from text
 * - Map statements to entities
 */
object EntityModule {

    const val VERSION = "1.0.0"
    const val NAME = "entity"

    private var isInitialized = false
    private var appContext: Context? = null
    
    private var entityDiscoverer: EntityDiscoverer? = null
    private var claimExtractor: ClaimExtractor? = null
    private var aliasResolver: AliasResolver? = null

    /**
     * Initialize the Entity module.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        entityDiscoverer = EntityDiscoverer()
        claimExtractor = ClaimExtractor()
        aliasResolver = AliasResolver()
        
        isInitialized = true
    }

    /**
     * Get entity discoverer.
     */
    fun getEntityDiscoverer(): EntityDiscoverer {
        return entityDiscoverer ?: throw IllegalStateException("EntityModule not initialized")
    }

    /**
     * Get claim extractor.
     */
    fun getClaimExtractor(): ClaimExtractor {
        return claimExtractor ?: throw IllegalStateException("EntityModule not initialized")
    }

    /**
     * Get alias resolver.
     */
    fun getAliasResolver(): AliasResolver {
        return aliasResolver ?: throw IllegalStateException("EntityModule not initialized")
    }
}
