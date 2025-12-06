package org.verumomnis.engine

/**
 * STEP 2: NARRATIVE BUILD
 * Merge text into a chronological narrative.
 * Output type: List<Sentence> narrativeList
 */
class NarrativeBuilder {
    
    /**
     * Build chronological narrative from raw sentences.
     * Preserves order and creates Sentence objects.
     */
    fun buildNarrative(rawSentences: List<String>, sourceEvidenceId: String = "default"): List<Sentence> {
        val narrativeList = mutableListOf<Sentence>()
        
        for ((index, text) in rawSentences.withIndex()) {
            narrativeList.add(
                Sentence(
                    text = text,
                    sourceEvidenceId = sourceEvidenceId,
                    timestamp = System.currentTimeMillis() + index // Simple ordering
                )
            )
        }
        
        return narrativeList
    }
}
