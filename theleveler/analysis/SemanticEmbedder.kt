package ai.verum.theleveler.analysis

import kotlin.math.sqrt

/**
 * Lightweight offline embedding engine.
 * Converts sentences into numeric vectors using:
 * - token hashing
 * - character n-grams
 * - stopword removal
 *
 * Not as strong as cloud embeddings, but extremely stable offline.
 */
object SemanticEmbedder {

    private val stopwords = setOf(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "must", "shall", "can", "need", "dare",
        "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
        "into", "through", "during", "before", "after", "above", "below",
        "between", "under", "again", "further", "then", "once", "here",
        "there", "when", "where", "why", "how", "all", "each", "few",
        "more", "most", "other", "some", "such", "no", "nor", "not",
        "only", "own", "same", "so", "than", "too", "very", "just",
        "and", "but", "if", "or", "because", "until", "while", "although",
        "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
        "you", "your", "yours", "yourself", "yourselves", "he", "him",
        "his", "himself", "she", "her", "hers", "herself", "it", "its",
        "itself", "they", "them", "their", "theirs", "themselves",
        "what", "which", "who", "whom", "this", "that", "these", "those",
        "am", "about", "against", "both", "any", "down", "up", "out",
        "off", "over", "own", "s", "t", "don", "now", "d", "ll", "m",
        "o", "re", "ve", "y", "ain", "aren", "couldn", "didn", "doesn",
        "hadn", "hasn", "haven", "isn", "ma", "mightn", "mustn", "needn",
        "shan", "shouldn", "wasn", "weren", "won", "wouldn"
    )

    /**
     * Embed text into a 256-dimension vector using token hashing.
     */
    fun embed(text: String): FloatArray {
        val tokens = text
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() && it.length > 2 && it !in stopwords }

        val vector = FloatArray(256)
        
        for (token in tokens) {
            // Primary hash
            val h = token.hashCode()
            val idx = (h and 0xFF)
            vector[idx] += 1f
            
            // Character n-gram hashing for better coverage
            if (token.length >= 3) {
                for (i in 0 until token.length - 2) {
                    val trigram = token.substring(i, i + 3)
                    val trigramIdx = (trigram.hashCode() and 0xFF)
                    vector[trigramIdx] += 0.5f
                }
            }
        }
        
        // Normalize vector
        val magnitude = sqrt(vector.map { it * it }.sum())
        if (magnitude > 0) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
        
        return vector
    }

    /**
     * Calculate cosine similarity between two vectors.
     */
    fun cosine(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "Vectors must have same dimension" }
        
        var dot = 0.0
        var magA = 0.0
        var magB = 0.0
        
        for (i in a.indices) {
            dot += (a[i] * b[i])
            magA += (a[i] * a[i])
            magB += (b[i] * b[i])
        }
        
        return if (magA == 0.0 || magB == 0.0) 0.0 
               else dot / (sqrt(magA) * sqrt(magB))
    }

    /**
     * Calculate semantic similarity between two text strings.
     */
    fun similarity(textA: String, textB: String): Double {
        val embA = embed(textA)
        val embB = embed(textB)
        return cosine(embA, embB)
    }
}
