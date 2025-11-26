package com.verumomnis.forensic.nlp

/**
 * NLP utilities for entity extraction, date parsing, and claim classification.
 * This is a basic offline implementation using regex patterns.
 */
object NLPUtil {

    // Common name patterns
    private val namePattern = Regex("""[A-Z][a-z]+\s[A-Z][a-z]+""")
    
    // Email pattern
    private val emailPattern = Regex("""[\w.-]+@[\w.-]+\.\w+""")
    
    // Phone pattern
    private val phonePattern = Regex("""\+?\d{1,3}[-.\s]?\(?\d{1,4}\)?[-.\s]?\d{1,4}[-.\s]?\d{1,9}""")
    
    // Date patterns
    private val datePatterns = listOf(
        Regex("""\d{1,2}/\d{1,2}/\d{2,4}"""),
        Regex("""\d{1,2}-\d{1,2}-\d{2,4}"""),
        Regex("""\d{1,2}\s(?:January|February|March|April|May|June|July|August|September|October|November|December)\s\d{4}""", RegexOption.IGNORE_CASE),
        Regex("""(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)""", RegexOption.IGNORE_CASE),
        Regex("""(?:yesterday|today|tomorrow|last week|next week|last month)""", RegexOption.IGNORE_CASE)
    )

    /**
     * Extracts entities (names, emails, phone numbers) from text.
     */
    fun extractEntities(text: String): List<String> {
        val entities = mutableListOf<String>()
        
        // Extract names
        namePattern.findAll(text).forEach { entities.add(it.value) }
        
        // Extract emails
        emailPattern.findAll(text).forEach { entities.add(it.value) }
        
        // Extract phone numbers
        phonePattern.findAll(text).forEach { entities.add(it.value) }
        
        return entities.distinct()
    }

    /**
     * Extracts date references from text.
     */
    fun extractDates(text: String): List<String> {
        val dates = mutableListOf<String>()
        datePatterns.forEach { pattern ->
            pattern.findAll(text).forEach { dates.add(it.value) }
        }
        return dates.distinct()
    }

    /**
     * Classifies the type of claim (denial, assertion, promise, etc.)
     */
    fun classifyClaim(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("never") || lowerText.contains("no ") || 
            lowerText.contains("didn't") || lowerText.contains("did not") ||
            lowerText.contains("wasn't") || lowerText.contains("was not") -> "denial"
            
            lowerText.contains("will ") || lowerText.contains("i promise") ||
            lowerText.contains("going to") || lowerText.contains("shall ") -> "promise"
            
            lowerText.contains("i received") || lowerText.contains("i got") ||
            lowerText.contains("i sent") || lowerText.contains("i paid") -> "action_claim"
            
            lowerText.contains("i think") || lowerText.contains("i believe") ||
            lowerText.contains("maybe") || lowerText.contains("perhaps") -> "opinion"
            
            else -> "assertion"
        }
    }
}
