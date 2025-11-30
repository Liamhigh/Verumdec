package ai.verum.theleveler.analysis

/**
 * Similarity utility that merges lexical + semantic signals.
 * Detects contradictions through multiple methods.
 */
object Similarity {

    // Semantic similarity thresholds
    const val THRESHOLD_RELATED = 0.25      // Minimum to consider texts related
    const val THRESHOLD_SIMILAR = 0.45      // Texts are discussing same topic
    const val THRESHOLD_VERY_SIMILAR = 0.70 // Texts are nearly identical in meaning

    // Negation and opposition patterns
    private val negationPatterns = listOf(
        "never", "did not", "didn't", "was not", "wasn't", "were not", "weren't",
        "cannot", "can't", "could not", "couldn't", "will not", "won't",
        "would not", "wouldn't", "should not", "shouldn't", "do not", "don't",
        "does not", "doesn't", "have not", "haven't", "has not", "hasn't",
        "had not", "hadn't", "is not", "isn't", "are not", "aren't",
        "no longer", "none", "nobody", "nothing", "nowhere", "neither",
        "impossible", "false", "untrue", "incorrect", "wrong", "denied",
        "refused", "rejected", "not", "no"
    )

    // Certainty indicators (high certainty)
    private val highCertaintyPatterns = listOf(
        "definitely", "certainly", "absolutely", "always", "never",
        "100%", "guaranteed", "positive", "sure", "certain",
        "undoubtedly", "without doubt", "clearly", "obviously",
        "i know", "i saw", "i witnessed", "i confirm", "i swear"
    )

    // Uncertainty indicators (low certainty)
    private val lowCertaintyPatterns = listOf(
        "maybe", "perhaps", "possibly", "might", "could be",
        "i think", "i believe", "i guess", "i assume", "i suppose",
        "not sure", "uncertain", "unsure", "probably", "likely",
        "don't remember", "can't recall", "don't know", "hard to say",
        "if i recall", "to my knowledge", "as far as i know"
    )

    /**
     * Calculate semantic similarity score between two texts.
     */
    fun semanticScore(a: String, b: String): Double {
        return SemanticEmbedder.similarity(a, b)
    }

    /**
     * Detect if there is lexical opposition between two texts.
     * One text contains negation while the other does not on the same topic.
     */
    fun lexicalOpposition(a: String, b: String): Boolean {
        val aLower = a.lowercase()
        val bLower = b.lowercase()

        for (pattern in negationPatterns) {
            val aContains = aLower.contains(pattern)
            val bContains = bLower.contains(pattern)
            
            // Opposition: one has negation, the other doesn't
            if (aContains != bContains) {
                return true
            }
        }
        return false
    }

    /**
     * Detect if two statements are direct contradictions.
     * They must be related (semantic similarity > threshold) and have opposing polarity.
     */
    fun isDirectContradiction(a: String, b: String): Boolean {
        val sem = semanticScore(a, b)
        
        // Too unrelated to be a contradiction
        if (sem < THRESHOLD_RELATED) return false
        
        // Check for lexical opposition
        return lexicalOpposition(a, b)
    }

    /**
     * Detect implicit contradiction through semantic drift.
     * Texts are related but have diverged significantly.
     */
    fun isImplicitContradiction(a: String, b: String): Boolean {
        val sem = semanticScore(a, b)
        
        // Related but not similar enough - potential drift
        return sem >= THRESHOLD_RELATED && sem < THRESHOLD_SIMILAR
    }

    /**
     * Calculate certainty level of a statement.
     * Returns value between 0.0 (uncertain) and 1.0 (certain)
     */
    fun certaintyLevel(text: String): Double {
        val lower = text.lowercase()
        
        var highMatches = 0
        var lowMatches = 0
        
        for (pattern in highCertaintyPatterns) {
            if (lower.contains(pattern)) highMatches++
        }
        
        for (pattern in lowCertaintyPatterns) {
            if (lower.contains(pattern)) lowMatches++
        }
        
        val total = highMatches + lowMatches
        if (total == 0) return 0.5 // neutral
        
        return highMatches.toDouble() / total
    }

    /**
     * Detect certainty drop between two statements.
     */
    fun hasCertaintyDrop(before: String, after: String): Boolean {
        val certBefore = certaintyLevel(before)
        val certAfter = certaintyLevel(after)
        
        // Significant drop in certainty (> 0.3)
        return (certBefore - certAfter) > 0.3
    }

    /**
     * Detect certainty rise between two statements.
     */
    fun hasCertaintyRise(before: String, after: String): Boolean {
        val certBefore = certaintyLevel(before)
        val certAfter = certaintyLevel(after)
        
        // Significant rise in certainty (> 0.3)
        return (certAfter - certBefore) > 0.3
    }
}
