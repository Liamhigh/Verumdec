package com.verumdec.engine

import android.content.Context
import com.verumdec.data.*
import java.security.MessageDigest
import java.util.*

/**
 * LEVELER ENGINE (v1.0)
 * -------------------------------------------
 * Unified orchestration layer for the entire
 * Verum Omnis contradiction + behavioral engine.
 *
 * This class:
 *  - Accepts raw extracted statements and evidence
 *  - Normalises timestamps + text
 *  - Builds semantic embeddings
 *  - Builds entity profiles
 *  - Builds timeline
 *  - Detects contradictions (semantic, factual, negation, timeline)
 *  - Detects behavioral anomalies
 *  - Packages into LevelerOutput
 *
 * This is the SINGLE entry point for unified analysis.
 */
class LevelerEngine(private val context: Context?) {

    companion object {
        const val VERSION = "1.0"
        const val ENGINE_NAME = "LEVELER"
    }

    // Internal state
    private val statementIndex = mutableMapOf<String, LevelerStatement>()
    private val entityProfiles = mutableMapOf<String, LevelerEntityProfile>()
    private val analysisLog = mutableListOf<String>()

    /**
     * Run the full Leveler analysis pipeline.
     * This is the unified entry point that combines all analysis engines.
     */
    fun process(
        evidence: List<Evidence>,
        entities: List<Entity>,
        timeline: List<TimelineEvent>,
        existingContradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>
    ): LevelerOutput {
        analysisLog.clear()
        analysisLog.add("LEVELER ENGINE v$VERSION - Analysis Started: ${Date()}")

        // 1. Extract and index all statements from evidence
        analysisLog.add("Stage 1: Indexing statements from ${evidence.size} evidence files")
        val statements = extractAndIndexStatements(evidence, entities)
        analysisLog.add("  - Indexed ${statements.size} statements")

        // 2. Build entity profiles with financial and temporal footprints
        analysisLog.add("Stage 2: Building entity profiles for ${entities.size} entities")
        buildEntityProfiles(statements, entities)
        analysisLog.add("  - Built ${entityProfiles.size} entity profiles")

        // 3. Generate semantic embeddings for similarity matching
        analysisLog.add("Stage 3: Generating semantic embeddings")
        generateEmbeddings(statements)
        analysisLog.add("  - Generated embeddings for ${statements.size} statements")

        // 4. Detect semantic contradictions (high similarity with opposite meaning)
        analysisLog.add("Stage 4: Detecting semantic contradictions")
        val semanticContradictions = detectSemanticContradictions(statements, entities, evidence)
        analysisLog.add("  - Found ${semanticContradictions.size} semantic contradictions")

        // 5. Detect entity-level contradictions
        analysisLog.add("Stage 5: Detecting entity-level contradictions")
        val entityContradictions = detectEntityContradictions(entities)
        analysisLog.add("  - Found ${entityContradictions.size} entity contradictions")

        // 6. Detect behavioral anomalies using linguistic drift analysis
        analysisLog.add("Stage 6: Analyzing behavioral anomalies")
        val behavioralAnomalies = detectBehavioralAnomalies(statements, entities)
        analysisLog.add("  - Found ${behavioralAnomalies.size} behavioral anomalies")

        // 7. Combine all contradictions (existing + new Leveler-detected)
        val allContradictions = mergeContradictions(
            existingContradictions,
            semanticContradictions,
            entityContradictions
        )

        // 8. Combine behavioral patterns
        val allBehavioralPatterns = behavioralPatterns + behavioralAnomalies

        // 9. Generate statistics
        val statistics = generateStatistics(
            statements, entities, timeline, allContradictions, allBehavioralPatterns
        )

        analysisLog.add("Stage 7: Generating statistics")
        analysisLog.add("  - Total contradictions: ${allContradictions.size}")
        analysisLog.add("  - Total behavioral patterns: ${allBehavioralPatterns.size}")
        analysisLog.add("LEVELER ENGINE - Analysis Complete: ${Date()}")

        return LevelerOutput(
            engineVersion = VERSION,
            engineName = ENGINE_NAME,
            processedAt = Date(),
            statements = statements,
            entityProfiles = entityProfiles.values.toList(),
            contradictions = allContradictions,
            behavioralPatterns = allBehavioralPatterns,
            statistics = statistics,
            analysisLog = analysisLog.toList(),
            scanHash = generateScanHash(statements, allContradictions)
        )
    }

    /**
     * Extract statements from evidence and index them.
     */
    private fun extractAndIndexStatements(
        evidence: List<Evidence>,
        entities: List<Entity>
    ): List<LevelerStatement> {
        val statements = mutableListOf<LevelerStatement>()

        for (ev in evidence) {
            val sentences = ev.extractedText.split(Regex("[.!?\\n]"))
                .filter { it.trim().length > 10 }

            for ((lineNumber, sentence) in sentences.withIndex()) {
                val trimmed = sentence.trim()

                // Determine speaker from evidence metadata or text patterns
                val speaker = determineSpeaker(trimmed, ev, entities)

                val statement = LevelerStatement(
                    id = UUID.randomUUID().toString(),
                    text = trimmed,
                    speaker = speaker,
                    documentId = ev.id,
                    documentName = ev.fileName,
                    lineNumber = lineNumber + 1,
                    timestamp = ev.metadata.creationDate?.time,
                    sentiment = analyzeSentiment(trimmed),
                    certainty = analyzeCertainty(trimmed),
                    embedding = null
                )

                statements.add(statement)
                statementIndex[statement.id] = statement
            }
        }

        return statements
    }

    /**
     * Determine the speaker of a statement.
     */
    private fun determineSpeaker(
        text: String,
        evidence: Evidence,
        entities: List<Entity>
    ): String {
        // Check for chat format: "Name: message"
        val chatMatch = Regex("^([^:]+):\\s*(.+)").find(text)
        if (chatMatch != null) {
            return chatMatch.groupValues[1].trim()
        }

        // Check sender from metadata
        evidence.metadata.sender?.let { sender ->
            entities.find { entity ->
                sender.contains(entity.primaryName, ignoreCase = true) ||
                entity.emails.any { sender.contains(it, ignoreCase = true) }
            }?.let { return it.primaryName }
        }

        // Check author from metadata
        evidence.metadata.author?.let { author ->
            entities.find { entity ->
                author.contains(entity.primaryName, ignoreCase = true)
            }?.let { return it.primaryName }
        }

        return "Unknown"
    }

    /**
     * Build entity profiles with financial figures and timeline footprints.
     */
    private fun buildEntityProfiles(statements: List<LevelerStatement>, entities: List<Entity>) {
        entityProfiles.clear()

        for (entity in entities) {
            val profile = LevelerEntityProfile(
                entityId = entity.id,
                name = entity.primaryName,
                aliases = entity.aliases,
                emails = entity.emails
            )

            // Find statements by this entity
            val entityStatements = statements.filter { stmt ->
                stmt.speaker.equals(entity.primaryName, ignoreCase = true) ||
                entity.aliases.any { stmt.speaker.equals(it, ignoreCase = true) }
            }

            profile.statementIds.addAll(entityStatements.map { it.id })
            profile.documentIds.addAll(entityStatements.map { it.documentId }.distinct())

            // Extract financial figures from statements
            for (stmt in entityStatements) {
                Regex("\\$?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?|\\d+(?:\\.\\d+)?").findAll(stmt.text).forEach { match ->
                    val amountStr = match.value.replace(",", "").replace("$", "")
                    amountStr.toDoubleOrNull()?.let { amount ->
                        if (amount > 0 && amount < 1_000_000_000) {
                            profile.financialFigures.add(
                                LevelerFinancialFigure(
                                    amount = amount,
                                    timestamp = stmt.timestamp,
                                    description = stmt.text.take(100),
                                    documentId = stmt.documentId
                                )
                            )
                        }
                    }
                }

                // Add timeline footprint
                stmt.timestamp?.let { profile.timelineFootprint.add(it) }
            }

            entityProfiles[entity.id] = profile
        }
    }

    /**
     * Generate semantic embeddings using TF-IDF-like approach.
     */
    private fun generateEmbeddings(statements: List<LevelerStatement>) {
        // Build vocabulary
        val vocabulary = mutableMapOf<String, Int>()
        val documentFrequency = mutableMapOf<String, Int>()

        for (stmt in statements) {
            val words = tokenize(stmt.text)
            val uniqueWords = words.toSet()
            for (word in uniqueWords) {
                documentFrequency[word] = (documentFrequency[word] ?: 0) + 1
            }
            for (word in words) {
                if (word !in vocabulary) {
                    vocabulary[word] = vocabulary.size
                }
            }
        }

        val vocabSize = vocabulary.size.coerceAtMost(500)
        val topWords = vocabulary.keys.take(vocabSize).toList()

        // Generate embeddings
        for (stmt in statements) {
            val words = tokenize(stmt.text)
            val wordCounts = words.groupingBy { it }.eachCount()
            val embedding = FloatArray(vocabSize)

            for ((index, word) in topWords.withIndex()) {
                val tf = wordCounts[word]?.toFloat() ?: 0f
                val df = documentFrequency[word]?.toFloat() ?: 1f
                val idf = kotlin.math.ln((statements.size + 1).toFloat() / (df + 1))
                embedding[index] = tf * idf
            }

            // Normalize
            val norm = kotlin.math.sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
            if (norm > 0) {
                for (i in embedding.indices) {
                    embedding[i] /= norm
                }
            }

            statementIndex[stmt.id]?.embedding = embedding
        }
    }

    /**
     * Detect semantic contradictions based on high similarity with opposite meaning.
     */
    private fun detectSemanticContradictions(
        statements: List<LevelerStatement>,
        entities: List<Entity>,
        evidence: List<Evidence>
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()

        // Negation patterns
        val negationWords = setOf("not", "never", "no", "didn't", "didn't", "wasn't", "won't", 
            "don't", "can't", "couldn't", "wouldn't", "shouldn't", "haven't", "hasn't", "hadn't")

        for (i in statements.indices) {
            val stmtA = statements[i]
            val embA = statementIndex[stmtA.id]?.embedding ?: continue

            for (j in (i + 1) until statements.size) {
                val stmtB = statements[j]
                val embB = statementIndex[stmtB.id]?.embedding ?: continue

                // Calculate cosine similarity
                val similarity = cosineSimilarity(embA, embB)

                // High similarity (> 0.5) with opposite sentiment or negation pattern
                if (similarity > 0.5) {
                    val hasOpposingSentiment = (stmtA.sentiment > 0 && stmtB.sentiment < 0) ||
                            (stmtA.sentiment < 0 && stmtB.sentiment > 0)

                    val textALower = stmtA.text.lowercase()
                    val textBLower = stmtB.text.lowercase()
                    val hasNegationA = negationWords.any { textALower.contains("$it ") }
                    val hasNegationB = negationWords.any { textBLower.contains("$it ") }
                    val hasOpposingNegation = hasNegationA != hasNegationB

                    if (hasOpposingSentiment || hasOpposingNegation) {
                        // Find entity for this contradiction
                        val entityId = entities.find { entity ->
                            stmtA.speaker.equals(entity.primaryName, ignoreCase = true) ||
                            stmtB.speaker.equals(entity.primaryName, ignoreCase = true)
                        }?.id ?: entities.firstOrNull()?.id ?: ""

                        // Convert to app data model Statement
                        val appStmtA = Statement(
                            id = stmtA.id,
                            entityId = entityId,
                            text = stmtA.text,
                            date = stmtA.timestamp?.let { Date(it) },
                            sourceEvidenceId = stmtA.documentId,
                            type = StatementType.CLAIM,
                            keywords = tokenize(stmtA.text).take(5)
                        )

                        val appStmtB = Statement(
                            id = stmtB.id,
                            entityId = entityId,
                            text = stmtB.text,
                            date = stmtB.timestamp?.let { Date(it) },
                            sourceEvidenceId = stmtB.documentId,
                            type = if (hasNegationB) StatementType.DENIAL else StatementType.CLAIM,
                            keywords = tokenize(stmtB.text).take(5)
                        )

                        val severity = when {
                            similarity > 0.8 && hasOpposingNegation -> Severity.CRITICAL
                            similarity > 0.7 -> Severity.HIGH
                            similarity > 0.6 -> Severity.MEDIUM
                            else -> Severity.LOW
                        }

                        contradictions.add(Contradiction(
                            entityId = entityId,
                            statementA = appStmtA,
                            statementB = appStmtB,
                            type = ContradictionType.DIRECT,
                            severity = severity,
                            description = "[LEVELER] Semantic contradiction detected: " +
                                    "High similarity (${String.format("%.2f", similarity)}) with opposing meaning. " +
                                    "Statement A: \"${stmtA.text.take(50)}...\" vs " +
                                    "Statement B: \"${stmtB.text.take(50)}...\"",
                            legalImplication = "LEVELER ENGINE: This semantic contradiction indicates potentially " +
                                    "inconsistent statements that may be legally relevant."
                        ))
                    }
                }
            }
        }

        return contradictions
    }

    /**
     * Detect entity-level contradictions based on financial figures and timeline.
     */
    private fun detectEntityContradictions(entities: List<Entity>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()

        for (entity in entities) {
            val profile = entityProfiles[entity.id] ?: continue

            // Check for conflicting financial figures
            val financialFigures = profile.financialFigures
            if (financialFigures.size >= 2) {
                val amounts = financialFigures.map { it.amount }.distinct()
                if (amounts.size > 1) {
                    val minAmount = amounts.minOrNull() ?: 0.0
                    val maxAmount = amounts.maxOrNull() ?: 0.0
                    val variance = (maxAmount - minAmount) / maxAmount

                    if (variance > 0.1) { // More than 10% difference
                        val stmt1 = financialFigures.first()
                        val stmt2 = financialFigures.last()

                        val appStmt1 = Statement(
                            entityId = entity.id,
                            text = stmt1.description,
                            sourceEvidenceId = stmt1.documentId,
                            type = StatementType.CLAIM,
                            keywords = listOf("financial", "amount", minAmount.toString())
                        )

                        val appStmt2 = Statement(
                            entityId = entity.id,
                            text = stmt2.description,
                            sourceEvidenceId = stmt2.documentId,
                            type = StatementType.CLAIM,
                            keywords = listOf("financial", "amount", maxAmount.toString())
                        )

                        contradictions.add(Contradiction(
                            entityId = entity.id,
                            statementA = appStmt1,
                            statementB = appStmt2,
                            type = ContradictionType.CROSS_DOCUMENT,
                            severity = if (variance > 0.5) Severity.HIGH else Severity.MEDIUM,
                            description = "[LEVELER] Financial figure discrepancy detected for ${entity.primaryName}: " +
                                    "Amounts range from $${String.format("%.2f", minAmount)} to $${String.format("%.2f", maxAmount)} " +
                                    "(${String.format("%.1f", variance * 100)}% variance)",
                            legalImplication = "LEVELER ENGINE: Inconsistent financial figures may indicate " +
                                    "misrepresentation or changing claims about monetary amounts."
                        ))
                    }
                }
            }
        }

        return contradictions
    }

    /**
     * Detect behavioral anomalies using linguistic drift analysis.
     */
    private fun detectBehavioralAnomalies(
        statements: List<LevelerStatement>,
        entities: List<Entity>
    ): List<BehavioralPattern> {
        val anomalies = mutableListOf<BehavioralPattern>()

        for (entity in entities) {
            val profile = entityProfiles[entity.id] ?: continue
            val entityStatements = statements.filter { it.id in profile.statementIds }
                .sortedBy { it.timestamp ?: 0L }

            if (entityStatements.size < 2) continue

            // Detect certainty drift (confidence changes over time)
            val certaintyValues = entityStatements.map { it.certainty }
            val certaintyVariance = calculateVariance(certaintyValues)

            if (certaintyVariance > 0.15) {
                val instances = entityStatements
                    .filter { it.certainty < 0.5 }
                    .map { "\"${it.text.take(80)}...\" (certainty: ${String.format("%.2f", it.certainty)})" }

                if (instances.isNotEmpty()) {
                    anomalies.add(BehavioralPattern(
                        entityId = entity.id,
                        type = BehaviorType.OVER_EXPLAINING,
                        instances = instances.take(5),
                        firstDetectedAt = Date(),
                        severity = if (certaintyVariance > 0.25) Severity.HIGH else Severity.MEDIUM
                    ))
                }
            }

            // Detect sentiment shifts (sudden changes in tone)
            val sentimentValues = entityStatements.map { it.sentiment }
            for (i in 1 until sentimentValues.size) {
                val shift = kotlin.math.abs(sentimentValues[i] - sentimentValues[i - 1])
                if (shift > 0.5) {
                    anomalies.add(BehavioralPattern(
                        entityId = entity.id,
                        type = BehaviorType.EMOTIONAL_MANIPULATION,
                        instances = listOf(
                            "[LEVELER] Detected sentiment shift from " +
                                    "${String.format("%.2f", sentimentValues[i - 1])} to " +
                                    "${String.format("%.2f", sentimentValues[i])}"
                        ),
                        firstDetectedAt = Date(),
                        severity = Severity.MEDIUM
                    ))
                    break
                }
            }
        }

        return anomalies
    }

    /**
     * Merge existing contradictions with Leveler-detected ones.
     */
    private fun mergeContradictions(
        existing: List<Contradiction>,
        semantic: List<Contradiction>,
        entity: List<Contradiction>
    ): List<Contradiction> {
        val all = mutableListOf<Contradiction>()
        all.addAll(existing)
        all.addAll(semantic)
        all.addAll(entity)
        return all.distinctBy { "${it.statementA.text.take(50)}-${it.statementB.text.take(50)}" }
    }

    /**
     * Generate statistics from the analysis.
     */
    private fun generateStatistics(
        statements: List<LevelerStatement>,
        entities: List<Entity>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>
    ): LevelerStatistics {
        return LevelerStatistics(
            totalStatements = statements.size,
            totalEntities = entities.size,
            totalTimelineEvents = timeline.size,
            totalContradictions = contradictions.size,
            totalBehavioralPatterns = behavioralPatterns.size,
            levelerDetectedContradictions = contradictions.count { 
                it.description.contains("[LEVELER]") 
            },
            semanticContradictions = contradictions.count { 
                it.description.contains("Semantic contradiction") 
            },
            financialContradictions = contradictions.count { 
                it.description.contains("Financial figure") 
            },
            criticalSeverityCount = contradictions.count { it.severity == Severity.CRITICAL },
            highSeverityCount = contradictions.count { it.severity == Severity.HIGH },
            mediumSeverityCount = contradictions.count { it.severity == Severity.MEDIUM },
            lowSeverityCount = contradictions.count { it.severity == Severity.LOW },
            entitiesWithContradictions = contradictions.map { it.entityId }.distinct().size,
            averageStatementsPerEntity = if (entities.isNotEmpty()) 
                statements.size.toFloat() / entities.size else 0f
        )
    }

    /**
     * Generate a hash of the scan for verification.
     */
    private fun generateScanHash(
        statements: List<LevelerStatement>,
        contradictions: List<Contradiction>
    ): String {
        val content = StringBuilder()
        content.append("LEVELER_V$VERSION|")
        content.append("STATEMENTS:${statements.size}|")
        content.append("CONTRADICTIONS:${contradictions.size}|")
        statements.take(10).forEach { content.append(it.text.take(20)) }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toString().toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // Utility functions

    private fun tokenize(text: String): List<String> {
        val stopWords = setOf("the", "a", "an", "is", "are", "was", "were", "be", "been",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as", "into",
            "and", "but", "if", "or", "because", "this", "that", "it", "its", "i", "my", "me")

        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
    }

    private fun analyzeSentiment(text: String): Double {
        val positiveWords = setOf("good", "great", "excellent", "happy", "pleased", "agree",
            "confirm", "yes", "paid", "received", "complete", "done", "thank", "appreciate")
        val negativeWords = setOf("bad", "wrong", "never", "not", "didn't", "no", "refuse",
            "deny", "lie", "fraud", "fake", "false", "failed", "problem", "issue", "complaint")

        val words = tokenize(text)
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        val total = positiveCount + negativeCount

        return if (total > 0) (positiveCount - negativeCount).toDouble() / total else 0.0
    }

    private fun analyzeCertainty(text: String): Double {
        val certainWords = setOf("definitely", "certainly", "absolutely", "always", "never",
            "must", "will", "confirmed", "guaranteed", "positive", "sure")
        val uncertainWords = setOf("maybe", "perhaps", "possibly", "might", "could", "think",
            "believe", "assume", "guess", "probably", "likely", "seems")

        val words = tokenize(text)
        val certainCount = words.count { it in certainWords }
        val uncertainCount = words.count { it in uncertainWords }

        return when {
            certainCount > uncertainCount -> 0.8 + (0.2 * certainCount / (certainCount + uncertainCount + 1))
            uncertainCount > certainCount -> 0.2 + (0.3 * uncertainCount / (certainCount + uncertainCount + 1))
            else -> 0.5
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) return 0.0

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (normA > 0 && normB > 0) {
            dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        } else 0.0
    }

    private fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average()
    }
}

// Data classes for Leveler Engine

/**
 * Statement as processed by the Leveler engine.
 */
data class LevelerStatement(
    val id: String,
    val text: String,
    val speaker: String,
    val documentId: String,
    val documentName: String,
    val lineNumber: Int,
    val timestamp: Long?,
    val sentiment: Double,
    val certainty: Double,
    var embedding: FloatArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LevelerStatement) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Entity profile built by the Leveler engine.
 */
data class LevelerEntityProfile(
    val entityId: String,
    val name: String,
    val aliases: List<String>,
    val emails: List<String>,
    val statementIds: MutableList<String> = mutableListOf(),
    val documentIds: MutableList<String> = mutableListOf(),
    val financialFigures: MutableList<LevelerFinancialFigure> = mutableListOf(),
    val timelineFootprint: MutableList<Long> = mutableListOf()
)

/**
 * Financial figure detected by the Leveler engine.
 */
data class LevelerFinancialFigure(
    val amount: Double,
    val timestamp: Long?,
    val description: String,
    val documentId: String
)

/**
 * Statistics from the Leveler analysis.
 */
data class LevelerStatistics(
    val totalStatements: Int,
    val totalEntities: Int,
    val totalTimelineEvents: Int,
    val totalContradictions: Int,
    val totalBehavioralPatterns: Int,
    val levelerDetectedContradictions: Int,
    val semanticContradictions: Int,
    val financialContradictions: Int,
    val criticalSeverityCount: Int,
    val highSeverityCount: Int,
    val mediumSeverityCount: Int,
    val lowSeverityCount: Int,
    val entitiesWithContradictions: Int,
    val averageStatementsPerEntity: Float
)

/**
 * Output from the Leveler engine analysis.
 */
data class LevelerOutput(
    val engineVersion: String,
    val engineName: String,
    val processedAt: Date,
    val statements: List<LevelerStatement>,
    val entityProfiles: List<LevelerEntityProfile>,
    val contradictions: List<Contradiction>,
    val behavioralPatterns: List<BehavioralPattern>,
    val statistics: LevelerStatistics,
    val analysisLog: List<String>,
    val scanHash: String
)
