package org.verumomnis.engine

/**
 * LAYER 3 — CLASSIFICATION ENGINE
 * 
 * Purpose: Map contradictions to legal subject categories defined in Verum Omnis.
 * This layer classifies contradictions into legal categories.
 */
class ClassificationEngine {
    
    /**
     * Legal subject categories
     */
    enum class LegalSubject {
        SHAREHOLDER_OPPRESSION,
        BREACH_OF_FIDUCIARY_DUTY,
        CYBERCRIME,
        FRAUDULENT_EVIDENCE,
        EMOTIONAL_EXPLOITATION
    }
    
    /**
     * Classify contradictions into legal subjects
     */
    fun classify(results: List<ContradictionEngine.ContradictionResult>): List<LegalFinding> {
        val findingsBySubject = mutableMapOf<LegalSubject, MutableList<ContradictionEngine.ContradictionResult>>()
        
        // Classify each contradiction
        for (contradiction in results) {
            val subjects = classifyContradiction(contradiction)
            
            for (subject in subjects) {
                findingsBySubject.getOrPut(subject) { mutableListOf() }.add(contradiction)
            }
        }
        
        // Build findings list
        return findingsBySubject.map { (subject, contradictions) ->
            LegalFinding(subject = subject, contradictions = contradictions)
        }
    }
    
    /**
     * Classify a single contradiction into one or more legal subjects
     */
    private fun classifyContradiction(contradiction: ContradictionEngine.ContradictionResult): List<LegalSubject> {
        val subjects = mutableListOf<LegalSubject>()
        val combinedText = "${contradiction.a.text} ${contradiction.b.text}".lowercase()
        
        // RULE A — Corporate / Business Conflicts → ShareholderOppression
        val shareholderKeywords = listOf("profit", "agreement", "deal", "decision", "responsibility", "ownership", 
            "shareholder", "dividend", "equity", "voting", "board")
        if (shareholderKeywords.any { combinedText.contains(it) }) {
            subjects.add(LegalSubject.SHAREHOLDER_OPPRESSION)
        }
        
        // RULE B — Evidence Tampering → FraudulentEvidence
        val fraudKeywords = listOf("delete", "removed", "cropped", "missing", "screenshot", "edited",
            "forged", "fabricated", "altered", "tampered")
        if (fraudKeywords.any { combinedText.contains(it) }) {
            subjects.add(LegalSubject.FRAUDULENT_EVIDENCE)
        }
        
        // RULE C — Device / Account Access → Cybercrime
        val cyberKeywords = listOf("access", "login", "password", "device", "breach", "unauthorized",
            "hacked", "cyber", "computer", "data")
        if (cyberKeywords.any { combinedText.contains(it) }) {
            subjects.add(LegalSubject.CYBERCRIME)
        }
        
        // RULE D — Trust / Duty Conflicts → BreachOfFiduciaryDuty
        val dutyKeywords = listOf("managing", "accounting", "decision-making", "duty", "lied",
            "fiduciary", "trust", "loyalty", "care", "director")
        if (dutyKeywords.any { combinedText.contains(it) }) {
            subjects.add(LegalSubject.BREACH_OF_FIDUCIARY_DUTY)
        }
        
        // RULE E — Manipulation / Denial → EmotionalExploitation
        val manipulationKeywords = listOf("gaslight", "you said", "you did", "never happened", "emotional",
            "imagining", "crazy", "sensitive", "overreacting")
        if (manipulationKeywords.any { combinedText.contains(it) }) {
            subjects.add(LegalSubject.EMOTIONAL_EXPLOITATION)
        }
        
        return subjects
    }
    
    /**
     * Data model for a legal finding
     */
    data class LegalFinding(
        val subject: LegalSubject,
        val contradictions: List<ContradictionEngine.ContradictionResult>
    )
}
