package com.verumdec.entity.extraction

import java.util.*

/**
 * ClaimExtractor - Extracts claims and statements from text.
 */
class ClaimExtractor {

    // Statement type indicators
    private val promiseIndicators = listOf(
        "i will", "i'll", "i promise", "i shall", "i'm going to",
        "we will", "we'll", "we promise", "we shall", "we're going to"
    )
    
    private val denialIndicators = listOf(
        "i never", "i didn't", "i did not", "i haven't", "i have not",
        "that's not true", "that is not true", "absolutely not", "no way",
        "never happened", "didn't happen", "i deny"
    )
    
    private val admissionIndicators = listOf(
        "i admit", "i confess", "to be honest", "the truth is",
        "i actually did", "yes i did", "i acknowledge", "i was wrong"
    )
    
    private val assertionIndicators = listOf(
        "the fact is", "actually", "in fact", "clearly", "obviously",
        "it's true that", "it is true that", "everyone knows"
    )

    /**
     * Extract all claims from text.
     */
    fun extract(text: String, entityId: String): ClaimExtractionResult {
        val claims = mutableListOf<ExtractedClaim>()
        val sentences = splitIntoSentences(text)
        
        for (sentence in sentences) {
            val lower = sentence.lowercase()
            
            // Check for promises
            for (indicator in promiseIndicators) {
                if (lower.contains(indicator)) {
                    claims.add(ExtractedClaim(
                        id = UUID.randomUUID().toString(),
                        entityId = entityId,
                        text = sentence.trim(),
                        type = ClaimType.PROMISE,
                        confidence = 0.85f,
                        indicator = indicator
                    ))
                    break
                }
            }
            
            // Check for denials
            for (indicator in denialIndicators) {
                if (lower.contains(indicator)) {
                    claims.add(ExtractedClaim(
                        id = UUID.randomUUID().toString(),
                        entityId = entityId,
                        text = sentence.trim(),
                        type = ClaimType.DENIAL,
                        confidence = 0.85f,
                        indicator = indicator
                    ))
                    break
                }
            }
            
            // Check for admissions
            for (indicator in admissionIndicators) {
                if (lower.contains(indicator)) {
                    claims.add(ExtractedClaim(
                        id = UUID.randomUUID().toString(),
                        entityId = entityId,
                        text = sentence.trim(),
                        type = ClaimType.ADMISSION,
                        confidence = 0.90f,
                        indicator = indicator
                    ))
                    break
                }
            }
            
            // Check for assertions
            for (indicator in assertionIndicators) {
                if (lower.contains(indicator)) {
                    claims.add(ExtractedClaim(
                        id = UUID.randomUUID().toString(),
                        entityId = entityId,
                        text = sentence.trim(),
                        type = ClaimType.ASSERTION,
                        confidence = 0.75f,
                        indicator = indicator
                    ))
                    break
                }
            }
        }
        
        return ClaimExtractionResult(
            entityId = entityId,
            claims = claims.distinctBy { it.text },
            totalSentences = sentences.size,
            extractedAt = Date()
        )
    }

    /**
     * Extract claims for multiple entities.
     */
    fun extractForEntities(
        text: String,
        entityIds: List<String>,
        entityMatcher: (String, String) -> Boolean
    ): Map<String, ClaimExtractionResult> {
        val results = mutableMapOf<String, ClaimExtractionResult>()
        val sentences = splitIntoSentences(text)
        
        for (entityId in entityIds) {
            val entityClaims = mutableListOf<ExtractedClaim>()
            
            for (sentence in sentences) {
                if (entityMatcher(sentence, entityId)) {
                    val extraction = extract(sentence, entityId)
                    entityClaims.addAll(extraction.claims)
                }
            }
            
            results[entityId] = ClaimExtractionResult(
                entityId = entityId,
                claims = entityClaims.distinctBy { it.text },
                totalSentences = sentences.size,
                extractedAt = Date()
            )
        }
        
        return results
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("[.!?\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 10 }
    }
}

enum class ClaimType {
    PROMISE, DENIAL, ADMISSION, ASSERTION, FACTUAL, OPINION
}

data class ExtractedClaim(
    val id: String,
    val entityId: String,
    val text: String,
    val type: ClaimType,
    val confidence: Float,
    val indicator: String
)

data class ClaimExtractionResult(
    val entityId: String,
    val claims: List<ExtractedClaim>,
    val totalSentences: Int,
    val extractedAt: Date
)
