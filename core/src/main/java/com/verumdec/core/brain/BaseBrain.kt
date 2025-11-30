package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.constitution.ConstitutionBrain
import com.verumdec.core.util.HashUtils
import java.util.*

/**
 * Base class for all Brains in the Verumdec system.
 * Provides common functionality and constitution enforcement.
 */
abstract class BaseBrain(protected val context: Context) {

    protected val constitutionBrain: ConstitutionBrain by lazy {
        ConstitutionBrain.getInstance(context)
    }

    /**
     * Brain identification.
     */
    abstract val brainName: String

    /**
     * Process input with constitution enforcement.
     */
    protected fun <T, R> processWithEnforcement(
        input: T,
        operationType: String,
        metadata: Map<String, Any> = emptyMap(),
        processor: (T) -> R
    ): BrainResult<R> {
        val startTime = System.currentTimeMillis()
        val enhancedMetadata = metadata.toMutableMap().apply {
            put("timestamp", startTime)
            put("brainName", brainName)
        }

        // Entry enforcement
        val entryResult = constitutionBrain.enforceOnEntry(brainName, operationType, enhancedMetadata)
        if (!entryResult.passed) {
            return BrainResult(
                success = false,
                data = null,
                errors = entryResult.warnings,
                warnings = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Process
        val result = try {
            processor(input)
        } catch (e: Exception) {
            return BrainResult(
                success = false,
                data = null,
                errors = listOf("Processing error: ${e.message}"),
                warnings = entryResult.warnings,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Exit enforcement
        val exitMetadata = enhancedMetadata.toMutableMap().apply {
            put("processedAt", System.currentTimeMillis())
        }
        val exitResult = constitutionBrain.enforceOnExit(brainName, operationType, result, exitMetadata)

        return BrainResult(
            success = true,
            data = result,
            errors = emptyList(),
            warnings = entryResult.warnings + exitResult.warnings,
            processingTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Generate unique processing ID.
     */
    protected fun generateProcessingId(): String {
        return HashUtils.generateHashId(brainName)
    }

    /**
     * Get current timestamp.
     */
    protected fun currentTimestamp(): Long = System.currentTimeMillis()
}

/**
 * Result container for Brain operations.
 */
data class BrainResult<T>(
    val success: Boolean,
    val data: T?,
    val errors: List<String>,
    val warnings: List<String>,
    val processingTimeMs: Long,
    val metadata: Map<String, Any> = emptyMap()
)
