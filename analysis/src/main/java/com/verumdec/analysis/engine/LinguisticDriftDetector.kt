package com.verumdec.analysis.engine

import com.verumdec.core.model.BehavioralAnomaly
import com.verumdec.core.model.BehavioralAnomalyType
import com.verumdec.core.model.Contradiction
import com.verumdec.core.model.ContradictionType
import com.verumdec.core.model.LegalTrigger
import com.verumdec.core.model.Statement
import com.verumdec.core.model.StatementIndex
import com.verumdec.entity.profile.BehavioralPattern
import com.verumdec.entity.profile.BehavioralProfile
import com.verumdec.entity.profile.CertaintyDataPoint
import com.verumdec.entity.profile.EntityProfile
import com.verumdec.entity.profile.SentimentDataPoint
import com.verumdec.entity.profile.ToneShift
import java.util.UUID
import kotlin.math.abs

/**
 * LinguisticDriftDetector performs behavioral and linguistic drift detection.
 *
 * For each speaker, it tracks:
 * - Sentiment trends
 * - Certainty markers
 * - Deflection language
 * - Change in tone before and after legal triggers
 * - Contradictions in behavior (e.g., sudden denial after prior certainty)
 *
 * Returns behavioral contradictions alongside factual contradictions.
 */
class LinguisticDriftDetector {
    
    // Keywords for detecting various behavioral patterns
    private val deflectionKeywords = listOf(
        "but", "however", "although", "that said", "to be fair",
        "i don't recall", "i can't remember", "i'm not sure",
        "you should ask", "that's not my department", "someone else",
        "it's complicated", "it depends", "technically speaking"
    )
    
    private val certaintyKeywords = listOf(
        "definitely", "certainly", "absolutely", "clearly", "obviously",
        "without doubt", "for sure", "guaranteed", "100%", "always",
        "never", "every time", "i know", "i'm certain", "factually"
    )
    
    private val uncertaintyKeywords = listOf(
        "maybe", "perhaps", "possibly", "might", "could", "uncertain",
        "not sure", "i think", "i believe", "i guess", "seems like",
        "probably", "likely", "unlikely", "unclear", "approximately"
    )
    
    private val defensiveKeywords = listOf(
        "i didn't", "i never", "that's not true", "you're wrong",
        "how dare you", "that's a lie", "i would never", "impossible",
        "ridiculous", "absurd", "i deny", "false", "no way"
    )
    
    private val cooperativeKeywords = listOf(
        "of course", "happy to help", "let me explain", "i agree",
        "absolutely", "yes", "certainly", "i understand", "fair point",
        "good question", "let's work together", "i appreciate"
    )
    
    private val overExplainingPatterns = listOf(
        "the reason is", "you see", "let me be clear", "to clarify",
        "what i meant was", "i should mention", "by the way",
        "also", "furthermore", "additionally", "and another thing"
    )
    
    private val blameShiftingPatterns = listOf(
        "it's because of", "they made me", "it's their fault",
        "i was forced", "i had no choice", "they told me to",
        "it wasn't my decision", "i was just following orders",
        "blame them", "they should have", "if only they"
    )
    
    private val gaslightingPatterns = listOf(
        "you're imagining", "that never happened", "you're confused",
        "you're being paranoid", "you're overreacting", "you're too sensitive",
        "that's not what i said", "you misunderstood", "you're crazy",
        "i never said that", "you're making things up"
    )
    
    /**
     * Detect all behavioral anomalies for speakers in the statement index.
     *
     * @param statementIndex Index of all statements
     * @param entityProfiles Entity profiles with behavioral data
     * @return List of behavioral anomalies
     */
    fun detectBehavioralAnomalies(
        statementIndex: StatementIndex,
        entityProfiles: Map<String, EntityProfile>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        
        for (speaker in statementIndex.getSpeakers()) {
            val statements = statementIndex.getStatementsBySpeaker(speaker)
                .sortedBy { it.timestampMillis ?: 0L }
            
            if (statements.size < 2) continue
            
            val profile = entityProfiles[speaker]
            
            // Detect various behavioral patterns
            anomalies.addAll(detectSentimentShifts(speaker, statements))
            anomalies.addAll(detectCertaintyChanges(speaker, statements))
            anomalies.addAll(detectToneShifts(speaker, statements))
            anomalies.addAll(detectDeflectionPatterns(speaker, statements))
            anomalies.addAll(detectOverExplaining(speaker, statements))
            anomalies.addAll(detectBlameShifting(speaker, statements))
            anomalies.addAll(detectGaslighting(speaker, statements))
            anomalies.addAll(detectSuddenDenial(speaker, statements))
            
            // Update entity profile if available
            profile?.let { updateBehavioralProfile(it, statements, anomalies.filter { a -> a.entityId == speaker }) }
        }
        
        return anomalies
    }
    
    /**
     * Convert behavioral anomalies to contradictions.
     */
    fun convertToContradictions(
        anomalies: List<BehavioralAnomaly>,
        statementIndex: StatementIndex
    ): List<Contradiction> {
        return anomalies.mapNotNull { anomaly ->
            val statements = anomaly.statementIds.mapNotNull { statementIndex.getStatement(it) }
            if (statements.size < 2) return@mapNotNull null
            
            val sourceStatement = statements.first()
            val targetStatement = statements.last()
            
            Contradiction(
                type = ContradictionType.BEHAVIORAL,
                sourceStatement = sourceStatement,
                targetStatement = targetStatement,
                sourceDocument = sourceStatement.documentId,
                sourceLineNumber = sourceStatement.lineNumber,
                severity = anomaly.severity,
                description = anomaly.description,
                legalTrigger = mapAnomalyToLegalTrigger(anomaly.type),
                affectedEntities = listOf(anomaly.entityId)
            )
        }
    }
    
    /**
     * Detect sentiment shifts in speaker statements.
     */
    private fun detectSentimentShifts(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        
        if (statements.size < 3) return anomalies
        
        // Calculate rolling sentiment
        for (i in 1 until statements.size) {
            val previousSentiment = statements[i - 1].sentiment
            val currentSentiment = statements[i].sentiment
            val shift = currentSentiment - previousSentiment
            
            // Significant negative shift
            if (shift < -0.5) {
                anomalies.add(
                    BehavioralAnomaly(
                        id = UUID.randomUUID().toString(),
                        entityId = speaker,
                        type = BehavioralAnomalyType.TONE_SHIFT,
                        description = "$speaker's tone shifted from ${describeSentiment(previousSentiment)} " +
                            "to ${describeSentiment(currentSentiment)}",
                        severity = calculateShiftSeverity(shift),
                        statementIds = listOf(statements[i - 1].id, statements[i].id),
                        beforeState = describeSentiment(previousSentiment),
                        afterState = describeSentiment(currentSentiment)
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Detect certainty changes in speaker statements.
     */
    private fun detectCertaintyChanges(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        
        if (statements.size < 3) return anomalies
        
        for (i in 1 until statements.size) {
            val previousCertainty = statements[i - 1].certainty
            val currentCertainty = statements[i].certainty
            val decline = previousCertainty - currentCertainty
            
            // Significant certainty decline
            if (decline > 0.3) {
                anomalies.add(
                    BehavioralAnomaly(
                        id = UUID.randomUUID().toString(),
                        entityId = speaker,
                        type = BehavioralAnomalyType.CONFIDENCE_DECLINE,
                        description = "$speaker's confidence declined from " +
                            "${describeCertainty(previousCertainty)} to ${describeCertainty(currentCertainty)}",
                        severity = calculateDeclineSeverity(decline),
                        statementIds = listOf(statements[i - 1].id, statements[i].id),
                        beforeState = describeCertainty(previousCertainty),
                        afterState = describeCertainty(currentCertainty)
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Detect tone shifts from cooperative to defensive.
     */
    private fun detectToneShifts(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        
        for (i in 1 until statements.size) {
            val prevTone = classifyTone(statements[i - 1].text)
            val currTone = classifyTone(statements[i].text)
            
            if (prevTone == "cooperative" && currTone == "defensive") {
                anomalies.add(
                    BehavioralAnomaly(
                        id = UUID.randomUUID().toString(),
                        entityId = speaker,
                        type = BehavioralAnomalyType.TONE_SHIFT,
                        description = "$speaker's tone shifted from cooperative to defensive",
                        severity = 6,
                        statementIds = listOf(statements[i - 1].id, statements[i].id),
                        beforeState = "cooperative",
                        afterState = "defensive"
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Detect deflection patterns.
     */
    private fun detectDeflectionPatterns(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        val deflectionStatements = mutableListOf<Statement>()
        
        for (statement in statements) {
            val deflectionCount = countMatches(statement.text, deflectionKeywords)
            if (deflectionCount >= 2) {
                deflectionStatements.add(statement)
            }
        }
        
        // If multiple deflection statements, flag as pattern
        if (deflectionStatements.size >= 2) {
            anomalies.add(
                BehavioralAnomaly(
                    id = UUID.randomUUID().toString(),
                    entityId = speaker,
                    type = BehavioralAnomalyType.DEFLECTION_PATTERN,
                    description = "$speaker shows repeated deflection behavior across ${deflectionStatements.size} statements",
                    severity = minOf(8, 4 + deflectionStatements.size),
                    statementIds = deflectionStatements.map { it.id },
                    beforeState = "direct",
                    afterState = "deflecting"
                )
            )
        }
        
        return anomalies
    }
    
    /**
     * Detect over-explaining (fraud red flag).
     */
    private fun detectOverExplaining(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        val overExplainingStatements = mutableListOf<Statement>()
        
        for (statement in statements) {
            val matchCount = countMatches(statement.text, overExplainingPatterns)
            if (matchCount >= 3 || statement.text.length > 500) {
                overExplainingStatements.add(statement)
            }
        }
        
        if (overExplainingStatements.size >= 2) {
            anomalies.add(
                BehavioralAnomaly(
                    id = UUID.randomUUID().toString(),
                    entityId = speaker,
                    type = BehavioralAnomalyType.OVER_EXPLAINING,
                    description = "$speaker shows over-explaining pattern (potential fraud indicator) " +
                        "in ${overExplainingStatements.size} statements",
                    severity = 7,
                    statementIds = overExplainingStatements.map { it.id },
                    beforeState = "concise",
                    afterState = "over-explaining"
                )
            )
        }
        
        return anomalies
    }
    
    /**
     * Detect blame shifting patterns.
     */
    private fun detectBlameShifting(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        val blameShiftingStatements = mutableListOf<Statement>()
        
        for (statement in statements) {
            val matchCount = countMatches(statement.text, blameShiftingPatterns)
            if (matchCount >= 1) {
                blameShiftingStatements.add(statement)
            }
        }
        
        if (blameShiftingStatements.size >= 2) {
            anomalies.add(
                BehavioralAnomaly(
                    id = UUID.randomUUID().toString(),
                    entityId = speaker,
                    type = BehavioralAnomalyType.BLAME_SHIFTING,
                    description = "$speaker shows blame-shifting behavior in ${blameShiftingStatements.size} statements",
                    severity = 6,
                    statementIds = blameShiftingStatements.map { it.id },
                    beforeState = "accountable",
                    afterState = "blame-shifting"
                )
            )
        }
        
        return anomalies
    }
    
    /**
     * Detect gaslighting behavior.
     */
    private fun detectGaslighting(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        val gaslightingStatements = mutableListOf<Statement>()
        
        for (statement in statements) {
            val matchCount = countMatches(statement.text, gaslightingPatterns)
            if (matchCount >= 1) {
                gaslightingStatements.add(statement)
            }
        }
        
        if (gaslightingStatements.isNotEmpty()) {
            anomalies.add(
                BehavioralAnomaly(
                    id = UUID.randomUUID().toString(),
                    entityId = speaker,
                    type = BehavioralAnomalyType.GASLIGHTING,
                    description = "$speaker uses gaslighting language in ${gaslightingStatements.size} statements",
                    severity = 8,
                    statementIds = gaslightingStatements.map { it.id },
                    beforeState = "neutral",
                    afterState = "gaslighting"
                )
            )
        }
        
        return anomalies
    }
    
    /**
     * Detect sudden denial after prior certainty.
     */
    private fun detectSuddenDenial(
        speaker: String,
        statements: List<Statement>
    ): List<BehavioralAnomaly> {
        val anomalies = mutableListOf<BehavioralAnomaly>()
        
        for (i in 1 until statements.size) {
            val prev = statements[i - 1]
            val curr = statements[i]
            
            val prevCertainty = countMatches(prev.text, certaintyKeywords)
            val currDefensive = countMatches(curr.text, defensiveKeywords)
            
            // Previous was certain, current is defensive/denial
            if (prevCertainty > 0 && currDefensive > 0 && prev.certainty > 0.7 && curr.certainty < 0.5) {
                anomalies.add(
                    BehavioralAnomaly(
                        id = UUID.randomUUID().toString(),
                        entityId = speaker,
                        type = BehavioralAnomalyType.SUDDEN_DENIAL,
                        description = "$speaker suddenly denied something they previously stated with certainty",
                        severity = 8,
                        statementIds = listOf(prev.id, curr.id),
                        beforeState = "certain assertion",
                        afterState = "sudden denial"
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Update an entity's behavioral profile.
     */
    private fun updateBehavioralProfile(
        profile: EntityProfile,
        statements: List<Statement>,
        anomalies: List<BehavioralAnomaly>
    ) {
        val behavioralProfile = profile.behavioralProfile
        
        // Update sentiment trend
        behavioralProfile.sentimentTrend.clear()
        statements.forEach { statement ->
            statement.timestampMillis?.let { ts ->
                behavioralProfile.sentimentTrend.add(
                    SentimentDataPoint(ts, statement.sentiment, statement.id)
                )
            }
        }
        
        // Update certainty trend
        behavioralProfile.certaintyTrend.clear()
        statements.forEach { statement ->
            statement.timestampMillis?.let { ts ->
                behavioralProfile.certaintyTrend.add(
                    CertaintyDataPoint(ts, statement.certainty, statement.id)
                )
            }
        }
        
        // Update deflection count
        behavioralProfile.deflectionCount = statements.sumOf { countMatches(it.text, deflectionKeywords) }
        
        // Update tone shifts
        behavioralProfile.toneShifts.clear()
        for (i in 1 until statements.size) {
            val prevTone = classifyTone(statements[i - 1].text)
            val currTone = classifyTone(statements[i].text)
            if (prevTone != currTone) {
                behavioralProfile.toneShifts.add(
                    ToneShift(
                        beforeStatementId = statements[i - 1].id,
                        afterStatementId = statements[i].id,
                        beforeTone = prevTone,
                        afterTone = currTone
                    )
                )
            }
        }
        
        // Update detected patterns
        behavioralProfile.patterns.clear()
        for (anomaly in anomalies) {
            val pattern = when (anomaly.type) {
                BehavioralAnomalyType.GASLIGHTING -> BehavioralPattern.GASLIGHTING
                BehavioralAnomalyType.DEFLECTION_PATTERN -> BehavioralPattern.DEFLECTION
                BehavioralAnomalyType.OVER_EXPLAINING -> BehavioralPattern.OVER_EXPLAINING
                BehavioralAnomalyType.BLAME_SHIFTING -> BehavioralPattern.BLAME_SHIFTING
                BehavioralAnomalyType.WITHDRAWAL -> BehavioralPattern.WITHDRAWAL
                BehavioralAnomalyType.PRESSURE_TACTICS -> BehavioralPattern.PRESSURE_TACTICS
                else -> null
            }
            pattern?.let { behavioralProfile.patterns.add(it) }
        }
    }
    
    /**
     * Count keyword matches in text using word boundary matching.
     */
    private fun countMatches(text: String, keywords: List<String>): Int {
        val lower = text.lowercase()
        return keywords.count { keyword ->
            // Use word boundary pattern to avoid substring false positives
            val pattern = Regex("\\b${Regex.escape(keyword)}\\b")
            pattern.containsMatchIn(lower)
        }
    }
    
    /**
     * Classify the tone of text.
     */
    private fun classifyTone(text: String): String {
        val lower = text.lowercase()
        val cooperativeCount = countMatches(lower, cooperativeKeywords)
        val defensiveCount = countMatches(lower, defensiveKeywords)
        
        return when {
            defensiveCount > cooperativeCount -> "defensive"
            cooperativeCount > defensiveCount -> "cooperative"
            else -> "neutral"
        }
    }
    
    /**
     * Describe sentiment as text.
     */
    private fun describeSentiment(sentiment: Double): String {
        return when {
            sentiment > 0.5 -> "positive"
            sentiment > 0.1 -> "slightly positive"
            sentiment > -0.1 -> "neutral"
            sentiment > -0.5 -> "slightly negative"
            else -> "negative"
        }
    }
    
    /**
     * Describe certainty as text.
     */
    private fun describeCertainty(certainty: Double): String {
        return when {
            certainty > 0.8 -> "very certain"
            certainty > 0.6 -> "moderately certain"
            certainty > 0.4 -> "somewhat uncertain"
            certainty > 0.2 -> "uncertain"
            else -> "very uncertain"
        }
    }
    
    /**
     * Calculate severity for sentiment shift.
     */
    private fun calculateShiftSeverity(shift: Double): Int {
        return when {
            abs(shift) > 1.5 -> 9
            abs(shift) > 1.0 -> 7
            abs(shift) > 0.5 -> 5
            else -> 3
        }
    }
    
    /**
     * Calculate severity for certainty decline.
     */
    private fun calculateDeclineSeverity(decline: Double): Int {
        return when {
            decline > 0.7 -> 8
            decline > 0.5 -> 6
            decline > 0.3 -> 4
            else -> 2
        }
    }
    
    /**
     * Map anomaly type to legal trigger.
     */
    private fun mapAnomalyToLegalTrigger(type: BehavioralAnomalyType): LegalTrigger {
        return when (type) {
            BehavioralAnomalyType.SUDDEN_DENIAL -> LegalTrigger.UNRELIABLE_TESTIMONY
            BehavioralAnomalyType.GASLIGHTING -> LegalTrigger.FRAUD
            BehavioralAnomalyType.OVER_EXPLAINING -> LegalTrigger.CONCEALMENT
            BehavioralAnomalyType.BLAME_SHIFTING -> LegalTrigger.MISREPRESENTATION
            BehavioralAnomalyType.DEFLECTION_PATTERN -> LegalTrigger.CONCEALMENT
            BehavioralAnomalyType.TONE_SHIFT -> LegalTrigger.UNRELIABLE_TESTIMONY
            BehavioralAnomalyType.CONFIDENCE_DECLINE -> LegalTrigger.UNRELIABLE_TESTIMONY
            else -> LegalTrigger.UNRELIABLE_TESTIMONY
        }
    }
}
