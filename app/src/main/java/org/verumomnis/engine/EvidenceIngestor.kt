package org.verumomnis.engine

/**
 * STEP 1: EVIDENCE INGESTION
 * Convert ALL evidence into text.
 * Output type: List<String> rawSentences
 */
class EvidenceIngestor {
    
    /**
     * Ingest all evidence and convert to plain text sentences.
     * This is the entry point for ALL evidence - no branching logic.
     */
    fun ingest(evidenceTexts: List<String>): List<String> {
        val rawSentences = mutableListOf<String>()
        
        for (text in evidenceTexts) {
            // Split text into sentences
            val sentences = splitIntoSentences(text)
            rawSentences.addAll(sentences)
        }
        
        return rawSentences.filter { it.isNotBlank() }
    }
    
    /**
     * Split text into sentences using basic punctuation rules.
     */
    private fun splitIntoSentences(text: String): List<String> {
        // Split on sentence-ending punctuation
        return text.split(Regex("[.!?]+\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
