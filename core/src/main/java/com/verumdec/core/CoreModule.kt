package com.verumdec.core

import android.content.Context
import com.verumdec.core.model.*
import com.verumdec.entity.profile.EntityProfile
import com.verumdec.entity.profile.EntityContradictionDetector
import com.verumdec.timeline.detection.TimelineContradictionDetector
import com.verumdec.analysis.engine.SemanticEmbeddingGenerator
import com.verumdec.analysis.engine.LinguisticDriftDetector
import java.util.UUID

/**
 * LEVELER ENGINE (v1.0)
 * -------------------------------------------
 * Unified orchestration layer for the entire
 * Verum Omnis contradiction + behavioral engine.
 *
 * This class:
 *  - Accepts raw extracted statements
 *  - Normalises timestamps + text
 *  - Builds semantic embeddings
 *  - Builds entity profiles
 *  - Builds timeline
 *  - Detects contradictions (semantic, factual, negation, timeline)
 *  - Detects behavioral anomalies
 *  - Packages into LevelerOutput
 *
 * This is the SINGLE entry point for the whole system.
 */
class Leveler(
    private val context: Context
) {

    // Core systems
    private val index = StatementIndex()
    private val embeddingGen = SemanticEmbeddingGenerator()
    private val timeline = TimelineContradictionDetector()
    private val entityProfiles = mutableMapOf<String, EntityProfile>()

    // Analysis engines
    private val driftDetector = LinguisticDriftDetector()
    private val entityContradictions = EntityContradictionDetector()

    /**
     * Run the full contradiction + behavioral pipeline.
     */
    fun process(statements: List<Statement>): LevelerOutput {

        // 1. Index all statements
        index.clear()
        index.addStatements(statements)

        // 2. Build entity profiles
        buildEntityProfiles(statements)

        // 3. Build embeddings
        generateEmbeddings(statements)

        // 4. Build timeline
        buildTimeline(statements)

        // 5. Run contradiction engines
        val semanticContradictions = detectSemanticContradictions()
        val timelineContradictions = timeline.detectTimelineContradictions()
        val entityContradictionsList = entityContradictions.detectEntityContradictions(entityProfiles)

        // 6. Behavioral engine
        val behavioralAnomalies = driftDetector.detectBehavioralAnomalies(index, entityProfiles)
        val behavioralContradictions = driftDetector.convertToContradictions(behavioralAnomalies, index)

        // 7. Package output
        return LevelerOutput(
            statements = index.getAllStatements(),
            timeline = timeline.buildMasterTimeline(),
            entityProfiles = entityProfiles.values.toList(),
            contradictions = (
                semanticContradictions +
                timelineContradictions +
                entityContradictionsList +
                behavioralContradictions
            ),
            anomalies = behavioralAnomalies,
            statistics = index.getStatistics()
        )
    }

    /**
     * Build entity profiles per speaker.
     */
    private fun buildEntityProfiles(statements: List<Statement>) {
        entityProfiles.clear()

        for (statement in statements) {
            val speaker = statement.speaker
            val profile = entityProfiles.getOrPut(speaker) {
                EntityProfile(name = speaker)
            }

            profile.statementIds.add(statement.id)
            profile.documentIds.add(statement.documentId)

            // Add financial references (if any)
            Regex("\\b\\d+(?:\\.\\d+)?\\b").findAll(statement.text).forEach {
                val amount = it.value.toDoubleOrNull() ?: return@forEach
                profile.addFinancialFigure(
                    com.verumdec.entity.profile.FinancialFigure(
                        amount = amount,
                        timestamp = statement.timestamp,
                        description = "Detected numeric value in statement",
                        documentId = statement.documentId
                    )
                )
            }

            // Add timeline footprint
            statement.timestamp?.let { ts ->
                profile.timelineFootprint.add(ts)
            }
        }
    }

    /**
     * Generate semantic embeddings for each statement.
     */
    private fun generateEmbeddings(statements: List<Statement>) {
        embeddingGen.buildVocabulary(statements.map { it.text })

        for (s in statements) {
            val embedding = embeddingGen.generateEmbedding(s.text)
            index.updateEmbedding(s.id, embedding)
        }
    }

    /**
     * Build timeline from statements.
     */
    private fun buildTimeline(statements: List<Statement>) {
        timeline.clear()
        timeline.buildTimelineFromStatements(statements)
    }

    /**
     * Detect semantic contradictions.
     */
    private fun detectSemanticContradictions(): List<Contradiction> {
        val results = mutableListOf<Contradiction>()
        val allStatements = index.getAllStatements()

        for (i in 0 until allStatements.size) {
            val a = allStatements[i]
            val embA = a.embedding ?: continue

            for (j in i + 1 until allStatements.size) {
                val b = allStatements[j]
                val embB = b.embedding ?: continue

                val match = embeddingGen.detectSemanticContradiction(
                    embA, embB,
                    a.text, b.text,
                    a.sentiment, b.sentiment
                )

                if (match != null) {
                    results.add(
                        Contradiction(
                            type = ContradictionType.SEMANTIC,
                            sourceStatement = a,
                            targetStatement = b,
                            sourceDocument = a.documentId,
                            sourceLineNumber = a.lineNumber,
                            severity = (match.contradictionScore * 10).toInt(),
                            description = match.reason,
                            legalTrigger = LegalTrigger.MISREPRESENTATION,
                            affectedEntities = listOf(a.speaker, b.speaker)
                        )
                    )
                }
            }
        }

        return results
    }
}

/**
 * Unified output from the LEVELER engine.
 */
data class LevelerOutput(
    val statements: List<Statement>,
    val timeline: List<TimelineEvent>,
    val entityProfiles: List<EntityProfile>,
    val contradictions: List<Contradiction>,
    val anomalies: List<BehavioralAnomaly>,
    val statistics: Map<String, Any>
)