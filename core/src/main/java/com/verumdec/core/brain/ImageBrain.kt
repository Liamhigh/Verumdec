package com.verumdec.core.brain

import android.content.Context
import android.graphics.Bitmap
import com.verumdec.core.util.HashUtils
import com.verumdec.core.util.ImageUtils
import java.io.File
import java.util.*

/**
 * ImageBrain - Analyzes image evidence.
 * Handles screenshots, photos, and scanned documents.
 */
class ImageBrain(context: Context) : BaseBrain(context) {

    override val brainName = "ImageBrain"

    /**
     * Analyze image file.
     */
    fun analyze(file: File): BrainResult<ImageAnalysis> {
        val hash = HashUtils.sha512(file)
        val metadata = mapOf(
            "fileName" to file.name,
            "sha512Hash" to hash,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(file, "ANALYZE_IMAGE", metadata) { imageFile ->
            val dimensions = ImageUtils.getImageDimensions(imageFile)
            val width = dimensions?.first ?: 0
            val height = dimensions?.second ?: 0

            ImageAnalysis(
                id = generateProcessingId(),
                fileName = imageFile.name,
                filePath = imageFile.absolutePath,
                contentHash = hash,
                width = width,
                height = height,
                aspectRatio = if (height > 0) width.toFloat() / height else 0f,
                megapixels = (width * height) / 1_000_000.0,
                resolutionLabel = ImageUtils.getResolutionLabel(width, height),
                isScreenshot = ImageUtils.isLikelyScreenshot(imageFile.name, width, height),
                fileSize = imageFile.length(),
                extension = imageFile.extension.lowercase(),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Analyze image from bitmap.
     */
    fun analyze(bitmap: Bitmap, fileName: String): BrainResult<ImageAnalysis> {
        val metadata = mapOf(
            "fileName" to fileName,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(bitmap, "ANALYZE_BITMAP", metadata) { bmp ->
            ImageAnalysis(
                id = generateProcessingId(),
                fileName = fileName,
                filePath = "",
                contentHash = "",
                width = bmp.width,
                height = bmp.height,
                aspectRatio = if (bmp.height > 0) bmp.width.toFloat() / bmp.height else 0f,
                megapixels = (bmp.width * bmp.height) / 1_000_000.0,
                resolutionLabel = ImageUtils.getResolutionLabel(bmp.width, bmp.height),
                isScreenshot = ImageUtils.isLikelyScreenshot(fileName, bmp.width, bmp.height),
                fileSize = 0,
                extension = fileName.substringAfterLast('.', "").lowercase(),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Extract text regions from image (for OCR preparation).
     */
    fun prepareForOcr(file: File, maxDimension: Int = 2048): BrainResult<OcrPreparation> {
        val metadata = mapOf(
            "fileName" to file.name,
            "sha512Hash" to HashUtils.sha512(file),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(file, "PREPARE_OCR", metadata) { imageFile ->
            val bitmap = ImageUtils.loadScaledBitmap(imageFile, maxDimension, maxDimension)
            
            OcrPreparation(
                id = generateProcessingId(),
                originalFile = imageFile.absolutePath,
                scaledWidth = bitmap?.width ?: 0,
                scaledHeight = bitmap?.height ?: 0,
                scaleFactor = calculateScaleFactor(imageFile, maxDimension),
                isReadyForOcr = bitmap != null,
                preparedAt = Date()
            )
        }
    }

    private fun calculateScaleFactor(file: File, maxDimension: Int): Float {
        val dimensions = ImageUtils.getImageDimensions(file) ?: return 1.0f
        val maxOriginal = maxOf(dimensions.first, dimensions.second)
        return if (maxOriginal > maxDimension) maxDimension.toFloat() / maxOriginal else 1.0f
    }
}

data class ImageAnalysis(
    val id: String,
    val fileName: String,
    val filePath: String,
    val contentHash: String,
    val width: Int,
    val height: Int,
    val aspectRatio: Float,
    val megapixels: Double,
    val resolutionLabel: String,
    val isScreenshot: Boolean,
    val fileSize: Long,
    val extension: String,
    val analyzedAt: Date
)

data class OcrPreparation(
    val id: String,
    val originalFile: String,
    val scaledWidth: Int,
    val scaledHeight: Int,
    val scaleFactor: Float,
    val isReadyForOcr: Boolean,
    val preparedAt: Date
)
