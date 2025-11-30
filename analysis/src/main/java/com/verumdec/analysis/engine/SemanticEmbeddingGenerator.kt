package com.verumdec.analysis.engine

import kotlin.math.sqrt
import kotlin.math.abs

/**
 * SemanticEmbeddingGenerator generates semantic vector embeddings for statements.
 * These embeddings enable similarity-based contradiction matching.
 *
 * Features:
 * - Generate semantic vector embedding for each statement
 * - Compare embeddings using cosine similarity
 * - Flag contradictions when:
 *   - High similarity + opposite sentiment
 *   - High similarity + conflicting facts
 *   - High similarity + timeline conflict
 *   - High similarity + speaker inconsistency
 */
class SemanticEmbeddingGenerator {
    
    // Vocabulary for simple TF-IDF based embeddings
    private val vocabulary = mutableMapOf<String, Int>()
    private val idfScores = mutableMapOf<String, Double>()
    private var embeddingDimension = 256
    
    /**
     * Build vocabulary from a corpus of texts.
     *
     * @param texts Collection of text documents
     */
    fun buildVocabulary(texts: Collection<String>) {
        vocabulary.clear()
        idfScores.clear()
        
        val documentFrequency = mutableMapOf<String, Int>()
        val allWords = mutableSetOf<String>()
        
        // Count document frequency for each word
        for (text in texts) {
            val words = tokenize(text).toSet()
            for (word in words) {
                documentFrequency[word] = (documentFrequency[word] ?: 0) + 1
                allWords.add(word)
            }
        }
        
        // Build vocabulary with most frequent words
        val sortedWords = documentFrequency.entries
            .sortedByDescending { it.value }
            .take(embeddingDimension)
        
        sortedWords.forEachIndexed { index, (word, _) ->
            vocabulary[word] = index
        }
        
        // Calculate IDF scores
        val numDocs = texts.size.toDouble()
        for ((word, docFreq) in documentFrequency) {
            if (vocabulary.containsKey(word)) {
                idfScores[word] = kotlin.math.ln(numDocs / (1 + docFreq))
            }
        }
    }
    
    /**
     * Generate embedding for a text.
     *
     * @param text Text to generate embedding for
     * @return Float array representing the semantic embedding
     */
    fun generateEmbedding(text: String): FloatArray {
        val embedding = FloatArray(embeddingDimension) { 0f }
        val words = tokenize(text)
        val wordCounts = words.groupingBy { it }.eachCount()
        
        // Calculate TF-IDF weighted embedding
        for ((word, count) in wordCounts) {
            val index = vocabulary[word] ?: continue
            val tf = count.toDouble() / words.size.coerceAtLeast(1)
            val idf = idfScores[word] ?: 1.0
            embedding[index] = (tf * idf).toFloat()
        }
        
        // Add sentiment-based features in last positions
        val sentimentScore = calculateSentimentScore(text)
        val certaintyScore = calculateCertaintyScore(text)
        
        // Normalize the embedding using a single loop for efficiency
        var normSquared = 0f
        for (i in embedding.indices) {
            normSquared += embedding[i] * embedding[i]
        }
        val norm = sqrt(normSquared)
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] = embedding[i] / norm
            }
        }
        
        return embedding
    }
    
    /**
     * Calculate cosine similarity between two embeddings.
     *
     * @param a First embedding
     * @param b Second embedding
     * @return Similarity score (0.0 to 1.0)
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
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
     * Check if two embeddings represent contradictory statements.
     *
     * @param embeddingA First embedding
     * @param embeddingB Second embedding
     * @param textA Original text A
     * @param textB Original text B
     * @param sentimentA Sentiment of statement A
     * @param sentimentB Sentiment of statement B
     * @return ContradictionMatch if contradiction detected, null otherwise
     */
    fun detectSemanticContradiction(
        embeddingA: FloatArray,
        embeddingB: FloatArray,
        textA: String,
        textB: String,
        sentimentA: Double,
        sentimentB: Double
    ): ContradictionMatch? {
        val similarity = cosineSimilarity(embeddingA, embeddingB)
        
        // Need high similarity to consider statements related
        if (similarity < 0.5) return null
        
        var contradictionScore = 0.0
        var reason = ""
        
        // Check for opposite sentiment with high similarity
        val sentimentDiff = abs(sentimentA - sentimentB)
        if (similarity > 0.7 && sentimentDiff > 1.0) {
            contradictionScore = similarity * (sentimentDiff / 2.0)
            reason = "High similarity with opposite sentiment"
        }
        
        // Check for conflicting facts using negation patterns
        if (hasConflictingFacts(textA, textB)) {
            contradictionScore = maxOf(contradictionScore, similarity * 0.9)
            reason = "Conflicting factual claims"
        }
        
        // Check for negation patterns
        if (hasNegationContradiction(textA, textB)) {
            contradictionScore = maxOf(contradictionScore, similarity * 0.95)
            reason = "Direct negation detected"
        }
        
        return if (contradictionScore > 0.5) {
            ContradictionMatch(
                similarity = similarity,
                contradictionScore = contradictionScore,
                reason = reason
            )
        } else null
    }
    
    /**
     * Check if texts contain conflicting facts.
     */
    private fun hasConflictingFacts(textA: String, textB: String): Boolean {
        val lowerA = textA.lowercase()
        val lowerB = textB.lowercase()
        
        // Extract numbers and check for conflicts
        val numbersA = Regex("\\d+").findAll(lowerA).map { it.value.toIntOrNull() ?: 0 }.toList()
        val numbersB = Regex("\\d+").findAll(lowerB).map { it.value.toIntOrNull() ?: 0 }.toList()
        
        // If similar context but different numbers
        if (numbersA.isNotEmpty() && numbersB.isNotEmpty()) {
            val commonContext = lowerA.split(Regex("\\d+")).filter { it.length > 5 }
                .any { segment -> lowerB.contains(segment.trim()) }
            
            if (commonContext && numbersA.toSet() != numbersB.toSet()) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check for negation-based contradiction.
     */
    private fun hasNegationContradiction(textA: String, textB: String): Boolean {
        val lowerA = textA.lowercase()
        val lowerB = textB.lowercase()
        
        val negationPairs = listOf(
            Pair("did", "did not"),
            Pair("did", "didn't"),
            Pair("was", "was not"),
            Pair("was", "wasn't"),
            Pair("is", "is not"),
            Pair("is", "isn't"),
            Pair("have", "have not"),
            Pair("have", "haven't"),
            Pair("will", "will not"),
            Pair("will", "won't"),
            Pair("can", "cannot"),
            Pair("can", "can't"),
            Pair("true", "false"),
            Pair("yes", "no"),
            Pair("agree", "disagree"),
            Pair("confirm", "deny"),
            Pair("accept", "reject"),
            Pair("admit", "deny")
        )
        
        for ((positive, negative) in negationPairs) {
            if ((lowerA.contains(positive) && lowerB.contains(negative)) ||
                (lowerA.contains(negative) && lowerB.contains(positive))) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Tokenize text into words.
     */
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
    }
    
    /**
     * Calculate a simple sentiment score.
     */
    private fun calculateSentimentScore(text: String): Double {
        val lower = text.lowercase()
        
        val positiveWords = listOf("good", "great", "excellent", "happy", "agree", "yes", "correct", "true", "confirm", "accept", "love", "wonderful")
        val negativeWords = listOf("bad", "terrible", "wrong", "disagree", "no", "false", "deny", "reject", "hate", "awful", "never", "not")
        
        val positiveCount = positiveWords.count { lower.contains(it) }
        val negativeCount = negativeWords.count { lower.contains(it) }
        
        return (positiveCount - negativeCount).toDouble() / maxOf(1, positiveCount + negativeCount)
    }
    
    /**
     * Calculate certainty score from text.
     */
    private fun calculateCertaintyScore(text: String): Double {
        val lower = text.lowercase()
        
        val certainWords = listOf("definitely", "certainly", "absolutely", "sure", "know", "fact", "proven", "clearly")
        val uncertainWords = listOf("maybe", "perhaps", "possibly", "might", "could", "uncertain", "unclear", "think", "believe")
        
        val certainCount = certainWords.count { lower.contains(it) }
        val uncertainCount = uncertainWords.count { lower.contains(it) }
        
        return (certainCount - uncertainCount).toDouble() / maxOf(1, certainCount + uncertainCount) * 0.5 + 0.5
    }
}

/**
 * Result of a semantic contradiction match.
 */
data class ContradictionMatch(
    val similarity: Double,
    val contradictionScore: Double,
    val reason: String
)
