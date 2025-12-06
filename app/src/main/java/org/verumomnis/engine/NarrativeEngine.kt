package org.verumomnis.engine

/**
 * LAYER 1 — NARRATIVE ENGINE
 * 
 * Purpose: Convert raw text evidence into clean, indexed, timestamp-aware sentences.
 * This layer normalizes evidence into structured sentences.
 */
class NarrativeEngine {
    
    /**
     * Ingest raw text and convert to structured sentences
     */
    fun ingest(rawText: String): List<Sentence> {
        return tokenize(rawText)
    }
    
    /**
     * Tokenize raw text into sentences using punctuation rules
     */
    private fun tokenize(rawText: String): List<Sentence> {
        val sentences = mutableListOf<Sentence>()
        
        // Split on sentence-ending punctuation
        val rawSentences = rawText.split(Regex("[.!?]+\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        // Create indexed sentences with optional timestamps
        rawSentences.forEachIndexed { index, text ->
            val timestamp = extractTimestamp(text)
            sentences.add(Sentence(text = text, index = index, timestamp = timestamp))
        }
        
        return sentences
    }
    
    /**
     * Attempt to extract timestamp from sentence
     * Detects: YYYY-MM-DD, HH:MM patterns
     */
    private fun extractTimestamp(sentence: String): Long? {
        // Pattern for date: YYYY-MM-DD
        val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
        val dateMatch = datePattern.find(sentence)
        
        if (dateMatch != null) {
            try {
                val dateParts = dateMatch.value.split("-")
                val year = dateParts[0].toInt()
                val month = dateParts[1].toInt()
                val day = dateParts[2].toInt()
                
                // Simple timestamp: days since epoch approximation
                return ((year - 1970) * 365L + (month - 1) * 30L + day) * 86400000L
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        
        return null
    }
    
    /**
     * Normalize sentence list (currently a pass-through, can add normalization logic)
     */
    fun normalize(sentences: List<Sentence>): List<Sentence> {
        return sentences
    }
    
    /**
     * Data model for a sentence with metadata
     */
    data class Sentence(
        val text: String,
        val index: Int,
        val timestamp: Long? = null
    )
}
