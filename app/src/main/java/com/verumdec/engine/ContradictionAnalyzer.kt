package com.verumdec.engine

import com.verumdec.data.*
import java.util.*

/**
 * Contradiction Analysis Engine (Gold Standard Implementation)
 * Detects contradictions between statements and across documents.
 * 
 * Based on the Verum Omnis forensic doctrine, this engine:
 * - Extracts claims with proper classification (denial, promise, admission, etc.)
 * - Detects direct contradictions (A says X, then A says NOT X)
 * - Detects cross-document contradictions
 * - Detects temporal/timeline contradictions
 * - Detects third-party contradictions
 * - Detects missing evidence contradictions
 * - Assigns severity scores (Critical, High, Medium, Low)
 * - Generates legal implications for each contradiction
 */
class ContradictionAnalyzer {

    // Negation indicators - comprehensive list for better contradiction detection
    private val negations = listOf(
        "not", "never", "no", "didn't", "wasn't", "won't", "don't", "can't", 
        "couldn't", "wouldn't", "shouldn't", "haven't", "hasn't", "hadn't",
        "nobody", "nothing", "nowhere", "none", "neither", "nor", "false",
        "denied", "denies", "deny", "refuses", "refused", "reject", "rejected"
    )
    
    // Contradiction trigger phrases - expanded with gold standard patterns
    private val contradictionTriggers = listOf(
        "deal exists" to "no deal",
        "paid" to "never paid",
        "received" to "never received",
        "sent" to "never sent",
        "agreed" to "never agreed",
        "signed" to "never signed",
        "promised" to "never promised",
        "true" to "false",
        "did" to "didn't",
        "did" to "did not",
        "was" to "wasn't",
        "was" to "was not",
        "have" to "haven't",
        "have" to "have not",
        "will" to "won't",
        "will" to "will not",
        "paid" to "didn't pay",
        "paid" to "did not pay",
        "received" to "didn't receive",
        "received" to "did not receive",
        "agreed" to "didn't agree",
        "agreed" to "did not agree",
        "yes" to "no",
        "accept" to "reject",
        "confirm" to "deny",
        "exists" to "doesn't exist",
        "happened" to "never happened",
        "said" to "never said",
        "knew" to "didn't know",
        "aware" to "unaware",
        "involved" to "not involved",
        "present" to "absent",
        "true" to "lie",
        "honest" to "dishonest",
        "admitted" to "denied"
    )
    
    // Slip-up admission patterns (Gold Standard feature)
    private val slipUpPatterns = listOf(
        Regex("okay[,\\s]+(fine|alright|maybe)", RegexOption.IGNORE_CASE),
        Regex("well[,\\s]+(technically|actually|maybe)", RegexOption.IGNORE_CASE),
        Regex("i\\s+(suppose|guess|admit)", RegexOption.IGNORE_CASE),
        Regex("(sort|kind)\\s+of", RegexOption.IGNORE_CASE),
        Regex("yes[,\\s]+but", RegexOption.IGNORE_CASE),
        Regex("i\\s+might\\s+have", RegexOption.IGNORE_CASE)
    )

    /**
     * Analyze all evidence for contradictions.
     */
    fun analyzeContradictions(
        evidenceList: List<Evidence>,
        entities: List<Entity>,
        timeline: List<TimelineEvent>
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Build statement map per entity
        val entityStatements = extractStatements(evidenceList, entities)
        
        for ((entityId, statements) in entityStatements) {
            // Check for direct contradictions within same entity
            contradictions.addAll(findDirectContradictions(entityId, statements))
            
            // Check for cross-document contradictions
            contradictions.addAll(findCrossDocumentContradictions(entityId, statements, evidenceList))
            
            // Check for temporal contradictions
            contradictions.addAll(findTemporalContradictions(entityId, statements, timeline))
        }
        
        // Check for third-party contradictions (entity A contradicts entity B)
        contradictions.addAll(findThirdPartyContradictions(entityStatements))
        
        // Check for missing evidence contradictions
        contradictions.addAll(findMissingEvidenceContradictions(entityStatements, evidenceList))
        
        return contradictions.distinctBy { "${it.statementA.id}-${it.statementB.id}" }
    }

    /**
     * Extract statements from evidence for each entity.
     */
    private fun extractStatements(
        evidenceList: List<Evidence>,
        entities: List<Entity>
    ): Map<String, List<Statement>> {
        val entityStatements = mutableMapOf<String, MutableList<Statement>>()
        
        for (evidence in evidenceList) {
            val sentences = evidence.extractedText.split(Regex("[.!?\\n]"))
                .filter { it.trim().length > 10 }
            
            for (sentence in sentences) {
                val trimmed = sentence.trim()
                
                // Find which entity made this statement
                val matchingEntity = entities.find { entity ->
                    isStatementByEntity(trimmed, entity, evidence)
                }
                
                if (matchingEntity != null) {
                    val statement = Statement(
                        entityId = matchingEntity.id,
                        text = trimmed,
                        date = evidence.metadata.creationDate,
                        sourceEvidenceId = evidence.id,
                        type = classifyStatement(trimmed),
                        keywords = extractKeywords(trimmed)
                    )
                    
                    entityStatements.getOrPut(matchingEntity.id) { mutableListOf() }.add(statement)
                }
            }
        }
        
        return entityStatements
    }

    /**
     * Check if a statement was made by a specific entity.
     */
    private fun isStatementByEntity(text: String, entity: Entity, evidence: Evidence): Boolean {
        // Check if sender matches
        val sender = evidence.metadata.sender?.lowercase() ?: ""
        if (entity.emails.any { sender.contains(it.lowercase()) }) return true
        if (sender.contains(entity.primaryName.lowercase())) return true
        
        // Check for first-person statements with entity context
        val firstPerson = text.lowercase().startsWith("i ") || 
                         text.lowercase().contains(" i ") ||
                         text.lowercase().contains(" my ")
        
        if (firstPerson) {
            // Check document author
            val author = evidence.metadata.author?.lowercase() ?: ""
            if (entity.primaryName.lowercase() in author) return true
            if (entity.aliases.any { it.lowercase() in author }) return true
        }
        
        // Check for WhatsApp/chat format: "Name: message"
        val chatMatch = Regex("^([^:]+):\\s*(.+)").find(text)
        if (chatMatch != null) {
            val speaker = chatMatch.groupValues[1].trim().lowercase()
            if (speaker == entity.primaryName.lowercase()) return true
            if (entity.aliases.any { it.lowercase() == speaker }) return true
        }
        
        return false
    }

    /**
     * Classify a statement by type (Gold Standard Implementation).
     * Uses comprehensive pattern matching for accurate claim classification.
     */
    private fun classifyStatement(text: String): StatementType {
        val lower = text.lowercase()
        
        return when {
            // Check for denial patterns - "never", "didn't", "not", etc. using word boundaries
            containsNegation(lower) -> StatementType.DENIAL
            
            // Check for slip-up admissions (Gold Standard feature)
            slipUpPatterns.any { it.containsMatchIn(lower) } -> StatementType.ADMISSION
            
            // Check for promise patterns
            lower.contains("i promise") || lower.contains("will ") || 
            lower.contains("shall ") || lower.contains("going to ") ||
            lower.contains("commit to") || lower.contains("guarantee") -> StatementType.PROMISE
            
            // Check for admission patterns
            lower.contains("admit") || lower.contains("yes i") || 
            lower.contains("i did") || lower.contains("confess") ||
            lower.contains("acknowledge") || lower.contains("i accept") ||
            lower.contains("guilty") || lower.contains("i was wrong") -> StatementType.ADMISSION
            
            // Check for claim patterns
            lower.contains("claim") || lower.contains("assert") ||
            lower.contains("state that") || lower.contains("i maintain") ||
            lower.contains("the fact is") || lower.contains("it is true that") -> StatementType.CLAIM
            
            // Check for accusation patterns  
            lower.contains("accuse") || lower.contains("fault") ||
            lower.contains("blame") || lower.contains("responsible for") ||
            lower.contains("you did") || lower.contains("he did") ||
            lower.contains("she did") || lower.contains("they did") -> StatementType.ACCUSATION
            
            // Check for explanation patterns
            lower.contains("because") || lower.contains("reason") ||
            lower.contains("explanation") || lower.contains("the thing is") ||
            lower.contains("what happened was") || lower.contains("let me explain") -> StatementType.EXPLANATION
            
            else -> StatementType.CLAIM
        }
    }

    /**
     * Extract keywords from a statement.
     */
    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "must", "shall", "can", "to", "of", "in", "for", "on", "with",
            "at", "by", "from", "as", "into", "through", "during", "before", "after",
            "above", "below", "between", "under", "again", "further", "then", "once",
            "here", "there", "when", "where", "why", "how", "all", "each", "few", "more",
            "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "just", "and", "but", "if", "or", "because",
            "until", "while", "this", "that", "these", "those", "it", "its", "i", "my", "me")
        
        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }

    /**
     * Find direct contradictions (A says X, then A says NOT X).
     */
    private fun findDirectContradictions(entityId: String, statements: List<Statement>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        for (i in statements.indices) {
            for (j in (i + 1) until statements.size) {
                val stmtA = statements[i]
                val stmtB = statements[j]
                
                val contradiction = checkDirectContradiction(stmtA, stmtB)
                if (contradiction != null) {
                    contradictions.add(Contradiction(
                        entityId = entityId,
                        statementA = stmtA,
                        statementB = stmtB,
                        type = ContradictionType.DIRECT,
                        severity = calculateSeverity(stmtA, stmtB),
                        description = contradiction,
                        legalImplication = generateLegalImplication(ContradictionType.DIRECT, stmtA, stmtB)
                    ))
                }
            }
        }
        
        return contradictions
    }

    /**
     * Check if two statements directly contradict each other.
     */
    private fun checkDirectContradiction(stmtA: Statement, stmtB: Statement): String? {
        val textA = stmtA.text.lowercase()
        val textB = stmtB.text.lowercase()
        
        // Check for negation patterns
        for ((positive, negative) in contradictionTriggers) {
            if (textA.contains(positive) && textB.contains(negative)) {
                return "Statement claims '$positive' but later statement indicates '$negative'"
            }
            if (textA.contains(negative) && textB.contains(positive)) {
                return "Statement claims '$negative' but later statement indicates '$positive'"
            }
        }
        
        // Check for keyword overlap with negation
        val keywordsA = stmtA.keywords.toSet()
        val keywordsB = stmtB.keywords.toSet()
        val commonKeywords = keywordsA.intersect(keywordsB)
        
        if (commonKeywords.isNotEmpty()) {
            val hasNegationA = containsNegation(textA)
            val hasNegationB = containsNegation(textB)
            
            if (hasNegationA != hasNegationB) {
                return "Contradictory statements about: ${commonKeywords.joinToString(", ")}"
            }
        }
        
        // Check denial vs claim pattern
        if (stmtA.type == StatementType.CLAIM && stmtB.type == StatementType.DENIAL) {
            if (commonKeywords.isNotEmpty()) {  // Reduced from >= 2 to improve detection
                return "Claim contradicted by denial"
            }
        }
        if (stmtA.type == StatementType.DENIAL && stmtB.type == StatementType.ADMISSION) {
            if (commonKeywords.isNotEmpty()) {
                return "Denial followed by admission"
            }
        }
        
        return null
    }
    
    /**
     * Check if text contains any negation word.
     */
    private fun containsNegation(text: String): Boolean {
        return negations.any { negation ->
            // Check for negation word with word boundaries
            val pattern = Regex("\\b${Regex.escape(negation)}\\b")
            pattern.containsMatchIn(text)
        }
    }

    /**
     * Find contradictions across different documents.
     */
    private fun findCrossDocumentContradictions(
        entityId: String,
        statements: List<Statement>,
        evidenceList: List<Evidence>
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Group statements by source document
        val byDocument = statements.groupBy { it.sourceEvidenceId }
        
        if (byDocument.size < 2) return contradictions
        
        val documentIds = byDocument.keys.toList()
        
        for (i in documentIds.indices) {
            for (j in (i + 1) until documentIds.size) {
                val statementsA = byDocument[documentIds[i]] ?: continue
                val statementsB = byDocument[documentIds[j]] ?: continue
                
                for (stmtA in statementsA) {
                    for (stmtB in statementsB) {
                        val contradiction = checkDirectContradiction(stmtA, stmtB)
                        if (contradiction != null) {
                            val evidenceA = evidenceList.find { it.id == documentIds[i] }
                            val evidenceB = evidenceList.find { it.id == documentIds[j] }
                            
                            contradictions.add(Contradiction(
                                entityId = entityId,
                                statementA = stmtA,
                                statementB = stmtB,
                                type = ContradictionType.CROSS_DOCUMENT,
                                severity = Severity.HIGH,
                                description = "Statement in ${evidenceA?.fileName ?: "document A"} contradicts statement in ${evidenceB?.fileName ?: "document B"}: $contradiction",
                                legalImplication = generateLegalImplication(ContradictionType.CROSS_DOCUMENT, stmtA, stmtB)
                            ))
                        }
                    }
                }
            }
        }
        
        return contradictions
    }

    /**
     * Find temporal contradictions (timeline inconsistencies).
     */
    private fun findTemporalContradictions(
        entityId: String,
        statements: List<Statement>,
        timeline: List<TimelineEvent>
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Look for statements about events that contradict timeline
        val datedStatements = statements.filter { it.date != null }.sortedBy { it.date }
        
        for (i in datedStatements.indices) {
            for (j in (i + 1) until datedStatements.size) {
                val earlier = datedStatements[i]
                val later = datedStatements[j]
                
                // Check for future reference that contradicts past claim
                if (earlier.type == StatementType.PROMISE && later.type == StatementType.DENIAL) {
                    val commonKeywords = earlier.keywords.intersect(later.keywords.toSet())
                    if (commonKeywords.isNotEmpty()) {
                        contradictions.add(Contradiction(
                            entityId = entityId,
                            statementA = earlier,
                            statementB = later,
                            type = ContradictionType.TEMPORAL,
                            severity = Severity.HIGH,
                            description = "Promise made on ${earlier.date} contradicted by denial on ${later.date}",
                            legalImplication = "This temporal inconsistency suggests deliberate deception or changing narrative."
                        ))
                    }
                }
            }
        }
        
        return contradictions
    }

    /**
     * Find contradictions between different entities.
     */
    private fun findThirdPartyContradictions(
        entityStatements: Map<String, List<Statement>>
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        val entityIds = entityStatements.keys.toList()
        
        for (i in entityIds.indices) {
            for (j in (i + 1) until entityIds.size) {
                val statementsA = entityStatements[entityIds[i]] ?: continue
                val statementsB = entityStatements[entityIds[j]] ?: continue
                
                for (stmtA in statementsA) {
                    for (stmtB in statementsB) {
                        // Check if statements are about the same topic but contradict
                        val commonKeywords = stmtA.keywords.intersect(stmtB.keywords.toSet())
                        if (commonKeywords.size >= 3) {
                            val contradiction = checkDirectContradiction(stmtA, stmtB)
                            if (contradiction != null) {
                                contradictions.add(Contradiction(
                                    entityId = entityIds[i],
                                    statementA = stmtA,
                                    statementB = stmtB,
                                    type = ContradictionType.THIRD_PARTY,
                                    severity = Severity.MEDIUM,
                                    description = "Statement contradicted by another party: $contradiction",
                                    legalImplication = "Third-party contradiction may indicate one party is being dishonest."
                                ))
                            }
                        }
                    }
                }
            }
        }
        
        return contradictions
    }

    /**
     * Find contradictions where entity references evidence that doesn't exist.
     */
    private fun findMissingEvidenceContradictions(
        entityStatements: Map<String, List<Statement>>,
        evidenceList: List<Evidence>
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        val evidenceReferences = listOf(
            Regex("attached (document|file|receipt|proof)", RegexOption.IGNORE_CASE),
            Regex("see (the )?(attached|enclosed)", RegexOption.IGNORE_CASE),
            Regex("as shown in", RegexOption.IGNORE_CASE),
            Regex("refer to (the )?(document|file|attachment)", RegexOption.IGNORE_CASE)
        )
        
        for ((entityId, statements) in entityStatements) {
            for (statement in statements) {
                for (pattern in evidenceReferences) {
                    if (pattern.containsMatchIn(statement.text)) {
                        // Check if referenced evidence exists
                        // This is simplified - real implementation would track actual attachments
                        val evidenceSource = evidenceList.find { it.id == statement.sourceEvidenceId }
                        if (evidenceSource != null && evidenceList.size == 1) {
                            // Only one document and it references attachments that aren't present
                            contradictions.add(Contradiction(
                                entityId = entityId,
                                statementA = statement,
                                statementB = statement, // Same statement - self contradiction
                                type = ContradictionType.MISSING_EVIDENCE,
                                severity = Severity.MEDIUM,
                                description = "Statement references evidence that was not provided: ${statement.text.take(100)}...",
                                legalImplication = "Missing referenced evidence may indicate fabrication or selective disclosure."
                            ))
                        }
                        break
                    }
                }
            }
        }
        
        return contradictions
    }

    /**
     * Calculate severity based on statement types and content.
     */
    private fun calculateSeverity(stmtA: Statement, stmtB: Statement): Severity {
        // Denial followed by admission is critical
        if (stmtA.type == StatementType.DENIAL && stmtB.type == StatementType.ADMISSION) {
            return Severity.CRITICAL
        }
        
        // Claim vs denial is high
        if ((stmtA.type == StatementType.CLAIM && stmtB.type == StatementType.DENIAL) ||
            (stmtA.type == StatementType.DENIAL && stmtB.type == StatementType.CLAIM)) {
            return Severity.HIGH
        }
        
        // Promise broken is medium
        if (stmtA.type == StatementType.PROMISE) {
            return Severity.MEDIUM
        }
        
        return Severity.LOW
    }

    /**
     * Generate legal implication text.
     */
    private fun generateLegalImplication(type: ContradictionType, stmtA: Statement, stmtB: Statement): String {
        return when (type) {
            ContradictionType.DIRECT -> "This direct contradiction indicates the party changed their position, suggesting unreliability or deliberate deception."
            ContradictionType.CROSS_DOCUMENT -> "Different accounts in different documents suggest the party has been inconsistent in their representation of facts."
            ContradictionType.BEHAVIORAL -> "Behavioral patterns indicate possible manipulation or strategic deception."
            ContradictionType.MISSING_EVIDENCE -> "Failure to provide referenced evidence may constitute withholding of material information."
            ContradictionType.TEMPORAL -> "Timeline inconsistency undermines the credibility of the party's account."
            ContradictionType.THIRD_PARTY -> "Contradiction by another party requires determination of which account is accurate."
        }
    }
}
