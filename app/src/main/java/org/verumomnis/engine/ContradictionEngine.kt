package org.verumomnis.engine

/**
 * LAYER 2 — CONTRADICTION ENGINE
 * 
 * Purpose: Detect contradictions between any two sentences.
 * This layer implements deterministic contradiction detection rules.
 */
class ContradictionEngine {
    
    /**
     * Analyze sentences and detect all contradictions
     */
    fun analyze(sentences: List<NarrativeEngine.Sentence>): List<ContradictionResult> {
        val contradictions = mutableListOf<ContradictionResult>()
        
        // Compare each pair of sentences
        for (i in sentences.indices) {
            for (j in i + 1 until sentences.size) {
                val a = sentences[i]
                val b = sentences[j]
                
                val reason = contradictionRule(a, b)
                if (reason != null) {
                    contradictions.add(ContradictionResult(a = a, b = b, reason = reason))
                }
            }
        }
        
        return contradictions
    }
    
    /**
     * Apply all contradiction rules to detect conflicts between two sentences
     */
    private fun contradictionRule(a: NarrativeEngine.Sentence, b: NarrativeEngine.Sentence): String? {
        val textA = a.text.lowercase()
        val textB = b.text.lowercase()
        
        // RULE 1 — Direct Negation
        val negationWords = listOf("never", "did not", "no", "not")
        val affirmationWords = listOf("yes", "did", "have", "was", "is", "agreed")
        
        if (negationWords.any { textA.contains(it) } && affirmationWords.any { textB.contains(it) }) {
            if (sharesTopic(textA, textB)) {
                return "Direct negation: one sentence denies while the other affirms the same event"
            }
        }
        if (negationWords.any { textB.contains(it) } && affirmationWords.any { textA.contains(it) }) {
            if (sharesTopic(textA, textB)) {
                return "Direct negation: one sentence denies while the other affirms the same event"
            }
        }
        
        // RULE 2 — Denial vs Evidence
        val denialPatterns = listOf("no payment", "never met", "did not send", "no meeting")
        val evidencePatterns = listOf("payment made", "we met", "sent", "meeting held")
        
        for (i in denialPatterns.indices) {
            if (textA.contains(denialPatterns[i]) && textB.contains(evidencePatterns[i])) {
                return "Denial contradicts evidence: '${denialPatterns[i]}' vs '${evidencePatterns[i]}'"
            }
            if (textB.contains(denialPatterns[i]) && textA.contains(evidencePatterns[i])) {
                return "Denial contradicts evidence: '${denialPatterns[i]}' vs '${evidencePatterns[i]}'"
            }
        }
        
        // RULE 3 — Timeline Conflicts
        if (a.timestamp != null && b.timestamp != null) {
            val topics = listOf("met", "invoice", "payment", "call", "meeting")
            if (topics.any { textA.contains(it) && textB.contains(it) }) {
                val daysDiff = Math.abs((a.timestamp - b.timestamp) / 86400000L)
                if (daysDiff > 30) { // More than 30 days difference
                    return "Timeline conflict: same event referenced with different dates (${daysDiff} days apart)"
                }
            }
        }
        
        // RULE 4 — Quantity Conflicts
        val quantities = extractQuantities(textA) to extractQuantities(textB)
        val topics = listOf("meeting", "payment", "invoice", "call", "email")
        
        if (quantities.first.isNotEmpty() && quantities.second.isNotEmpty()) {
            for (topic in topics) {
                if (textA.contains(topic) && textB.contains(topic)) {
                    if (quantities.first != quantities.second) {
                        return "Quantity conflict: different numbers for same subject ('${quantities.first.joinToString()}' vs '${quantities.second.joinToString()}')"
                    }
                }
            }
        }
        
        // RULE 5 — Admission vs Later Denial
        val admissionWords = listOf("i agreed", "i admitted", "i accept", "i confirm")
        val denialWords = listOf("i never agreed", "i never admitted", "i reject", "i deny")
        
        if (admissionWords.any { textA.contains(it) } && denialWords.any { textB.contains(it) }) {
            return "Admission contradicts later denial"
        }
        if (denialWords.any { textA.contains(it) } && admissionWords.any { textB.contains(it) }) {
            return "Denial contradicts later admission"
        }
        
        // RULE 6 — Action vs Outcome Conflict
        if ((textA.contains("sent nothing") || textA.contains("did not send")) && 
            (textB.contains("email attached") || textB.contains("document sent"))) {
            return "Action vs outcome conflict: claim of no action contradicts evidence of action"
        }
        if ((textB.contains("sent nothing") || textB.contains("did not send")) && 
            (textA.contains("email attached") || textA.contains("document sent"))) {
            return "Action vs outcome conflict: claim of no action contradicts evidence of action"
        }
        
        // RULE 7 — Data Access Claim Conflicts
        val noAccessClaims = listOf("did not access", "never accessed", "no access")
        val accessEvidence = listOf("logged in", "access attempt", "accessed", "login successful")
        
        if (noAccessClaims.any { textA.contains(it) } && accessEvidence.any { textB.contains(it) }) {
            return "Access claim conflict: denial of access contradicts evidence of access attempt"
        }
        if (noAccessClaims.any { textB.contains(it) } && accessEvidence.any { textA.contains(it) }) {
            return "Access claim conflict: denial of access contradicts evidence of access attempt"
        }
        
        return null
    }
    
    /**
     * Check if two texts share a common topic
     */
    private fun sharesTopic(textA: String, textB: String): Boolean {
        val topics = listOf("payment", "meeting", "invoice", "agreement", "call", "email", "document")
        return topics.any { textA.contains(it) && textB.contains(it) }
    }
    
    /**
     * Extract numerical quantities from text
     */
    private fun extractQuantities(text: String): List<Int> {
        val numberWords = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
        )
        
        val quantities = mutableListOf<Int>()
        
        // Extract digit numbers
        val digitPattern = Regex("""\d+""")
        digitPattern.findAll(text).forEach { match ->
            match.value.toIntOrNull()?.let { quantities.add(it) }
        }
        
        // Extract word numbers
        for ((word, num) in numberWords) {
            if (text.contains(word)) {
                quantities.add(num)
            }
        }
        
        return quantities
    }
    
    /**
     * Data model for a contradiction result
     */
    data class ContradictionResult(
        val a: NarrativeEngine.Sentence,
        val b: NarrativeEngine.Sentence,
        val reason: String
    )
}
