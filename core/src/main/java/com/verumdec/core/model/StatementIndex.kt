package com.verumdec.core.model

import kotlin.math.sqrt

/**
 * StatementIndex provides indexing and searching capabilities for statements.
 * This is the central system for statement storage and retrieval that enables
 * contradiction detection across documents.
 *
 * Key features:
 * - Unique ID assignment to every extracted statement
 * - Searchable index by speaker, document, timestamp, and content
 * - Semantic embeddings for similarity-based contradiction matching
 * - Reverse linking for contradiction origin/destination tracking
 */
class StatementIndex {
    
    private val statements = mutableMapOf<String, Statement>()
    private val bySpeaker = mutableMapOf<String, MutableList<String>>()
    private val byDocument = mutableMapOf<String, MutableList<String>>()
    private val byTimestampMillis = sortedMapOf<Long, MutableList<String>>()
    private val byTimestamp = mutableMapOf<String, MutableList<String>>()
    private val byLegalCategory = mutableMapOf<LegalCategory, MutableList<String>>()
    
    /**
     * Check if statements have been indexed.
     */
    fun hasStatements(): Boolean = statements.isNotEmpty()
    
    /**
     * Check if embeddings have been generated for statements.
     */
    fun hasEmbeddings(): Boolean = statements.values.any { it.embedding != null }
    
    /**
     * Get total count of indexed statements.
     */
    fun size(): Int = statements.size
    
    /**
     * Add a statement to the index.
     *
     * @param statement The statement to index
     */
    fun addStatement(statement: Statement) {
        statements[statement.id] = statement
        
        // Index by speaker
        bySpeaker.getOrPut(statement.speaker) { mutableListOf() }.add(statement.id)
        
        // Index by document
        byDocument.getOrPut(statement.documentId) { mutableListOf() }.add(statement.id)
        
        // Index by timestamp millis if available (for sorting)
        statement.timestampMillis?.let { ts ->
            byTimestampMillis.getOrPut(ts) { mutableListOf() }.add(statement.id)
        }
        
        // Index by string timestamp if available
        statement.timestamp?.let { ts ->
            byTimestamp.getOrPut(ts) { mutableListOf() }.add(statement.id)
        }
        
        // Index by legal category
        byLegalCategory.getOrPut(statement.legalCategory) { mutableListOf() }.add(statement.id)
    }
    
    /**
     * Add multiple statements to the index.
     *
     * @param statementList List of statements to index
     */
    fun addStatements(statementList: List<Statement>) {
        statementList.forEach { addStatement(it) }
    }
    
    /**
     * Retrieve a statement by ID.
     *
     * @param id Statement ID
     * @return Statement if found, null otherwise
     */
    fun getStatement(id: String): Statement? = statements[id]
    
    /**
     * Get all statements.
     *
     * @return List of all indexed statements
     */
    fun getAllStatements(): List<Statement> = statements.values.toList()
    
    /**
     * Get statements by speaker.
     *
     * @param speaker Speaker identifier
     * @return List of statements made by the speaker
     */
    fun getStatementsBySpeaker(speaker: String): List<Statement> {
        return bySpeaker[speaker]?.mapNotNull { statements[it] } ?: emptyList()
    }
    
    /**
     * Get all unique speakers.
     *
     * @return Set of speaker identifiers
     */
    fun getSpeakers(): Set<String> = bySpeaker.keys.toSet()
    
    /**
     * Get statements by document.
     *
     * @param documentId Document identifier
     * @return List of statements from the document
     */
    fun getStatementsByDocument(documentId: String): List<Statement> {
        return byDocument[documentId]?.mapNotNull { statements[it] } ?: emptyList()
    }
    
    /**
     * Get all unique document IDs.
     *
     * @return Set of document identifiers
     */
    fun getDocuments(): Set<String> = byDocument.keys.toSet()
    
    /**
     * Get statements within a time range.
     *
     * @param startTime Start timestamp (inclusive) in epoch millis
     * @param endTime End timestamp (inclusive) in epoch millis
     * @return List of statements within the time range
     */
    fun getStatementsByTimeRange(startTime: Long, endTime: Long): List<Statement> {
        return byTimestampMillis.subMap(startTime, endTime + 1)
            .values
            .flatten()
            .mapNotNull { statements[it] }
    }
    
    /**
     * Get statements by legal category.
     *
     * @param category Legal category
     * @return List of statements in the category
     */
    fun getStatementsByCategory(category: LegalCategory): List<Statement> {
        return byLegalCategory[category]?.mapNotNull { statements[it] } ?: emptyList()
    }
    
    /**
     * Find semantically similar statements using cosine similarity.
     *
     * @param statement The statement to find similar matches for
     * @param threshold Minimum similarity threshold (0.0 to 1.0)
     * @param maxResults Maximum number of results to return
     * @return List of pairs (statement, similarity score) sorted by similarity
     */
    fun findSimilarStatements(
        statement: Statement,
        threshold: Double = 0.7,
        maxResults: Int = 10
    ): List<Pair<Statement, Double>> {
        val embedding = statement.embedding ?: return emptyList()
        
        return statements.values
            .filter { it.id != statement.id && it.embedding != null }
            .mapNotNull { other ->
                val similarity = cosineSimilarity(embedding, other.embedding!!)
                if (similarity >= threshold) Pair(other, similarity) else null
            }
            .sortedByDescending { it.second }
            .take(maxResults)
    }
    
    /**
     * Search statements containing specific text.
     *
     * @param query Search query
     * @param caseSensitive Whether search is case-sensitive
     * @return List of matching statements
     */
    fun searchStatements(query: String, caseSensitive: Boolean = false): List<Statement> {
        val searchQuery = if (caseSensitive) query else query.lowercase()
        return statements.values.filter { statement ->
            val text = if (caseSensitive) statement.text else statement.text.lowercase()
            text.contains(searchQuery)
        }
    }
    
    /**
     * Update a statement's embedding.
     *
     * @param statementId Statement ID
     * @param embedding New embedding vector
     */
    fun updateEmbedding(statementId: String, embedding: FloatArray) {
        statements[statementId]?.let { statement ->
            statements[statementId] = statement.copy(embedding = embedding)
        }
    }
    
    /**
     * Clear the entire index.
     */
    fun clear() {
        statements.clear()
        bySpeaker.clear()
        byDocument.clear()
        byTimestampMillis.clear()
        byTimestamp.clear()
        byLegalCategory.clear()
    }
    
    /**
     * Calculate cosine similarity between two embedding vectors.
     *
     * @param a First embedding
     * @param b Second embedding
     * @return Cosine similarity (0.0 to 1.0)
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) return 0.0
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0.0
    }
    
    /**
     * Get statistics about the index.
     *
     * @return Map of statistic names to values
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "totalStatements" to statements.size,
            "uniqueSpeakers" to bySpeaker.size,
            "uniqueDocuments" to byDocument.size,
            "statementsWithEmbeddings" to statements.values.count { it.embedding != null },
            "statementsWithTimestamps" to statements.values.count { it.timestamp != null },
            "categoryCounts" to byLegalCategory.mapValues { it.value.size }
        )
    }
}
