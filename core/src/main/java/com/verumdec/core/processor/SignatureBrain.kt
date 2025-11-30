package com.verumdec.core.processor

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * SignatureBrain - Detect mismatched handwriting or copy-paste signatures
 *
 * Analyzes signature images to detect potential forgery, copy-paste signatures,
 * digital overlays, and other manipulation indicators.
 *
 * Operates fully offline without external dependencies.
 */
class SignatureBrain {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif"
        )

        // Signature analysis thresholds
        private const val MIN_SIGNATURE_WIDTH = 50
        private const val MIN_SIGNATURE_HEIGHT = 20
        private const val UNIFORM_BACKGROUND_THRESHOLD = 0.95f
        private const val EDGE_SHARPNESS_THRESHOLD = 0.8f

        // Bounding box estimation constants
        private const val SIZE_TO_WIDTH_RATIO = 0.5 // Estimate width from file size
        private const val MIN_ESTIMATED_WIDTH = 100
        private const val MAX_ESTIMATED_WIDTH = 1000
        private const val WIDTH_TO_HEIGHT_RATIO = 3 // Signatures are typically 3x wider than tall
        private const val DEFAULT_MARGIN = 10
    }

    // Store reference signatures for comparison
    private val referenceSignatures = mutableMapOf<String, SignatureProfile>()

    /**
     * Signature profile for comparison.
     */
    data class SignatureProfile(
        val id: String,
        val hash: String,
        val strokeAnalysis: StrokeAnalysis?,
        val dimensions: BoundingBox,
        val averageIntensity: Float
    )

    /**
     * Analyze a signature image.
     *
     * @param file The signature image file
     * @param referenceId Optional reference signature ID for comparison
     * @return SignatureBrainResult containing analysis or error
     */
    fun analyze(file: File, referenceId: String? = null): SignatureBrainResult {
        return try {
            if (!file.exists()) {
                return SignatureBrainResult.Failure(
                    error = "File not found: ${file.absolutePath}",
                    errorCode = SignatureErrorCode.SIGNATURE_NOT_FOUND
                )
            }

            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return SignatureBrainResult.Failure(
                    error = "Unsupported image format: $extension",
                    errorCode = SignatureErrorCode.IMAGE_QUALITY_TOO_LOW
                )
            }

            val bytes = file.readBytes()
            analyzeBytes(bytes, extension, referenceId)
        } catch (e: Exception) {
            SignatureBrainResult.Failure(
                error = "Analysis error: ${e.message}",
                errorCode = SignatureErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Analyze a signature from an input stream.
     *
     * @param inputStream The input stream containing the signature image
     * @param fileName The original filename for extension detection
     * @param referenceId Optional reference signature ID for comparison
     * @return SignatureBrainResult containing analysis or error
     */
    fun analyze(inputStream: InputStream, fileName: String, referenceId: String? = null): SignatureBrainResult {
        return try {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return SignatureBrainResult.Failure(
                    error = "Unsupported image format: $extension",
                    errorCode = SignatureErrorCode.IMAGE_QUALITY_TOO_LOW
                )
            }

            val bytes = inputStream.readBytes()
            analyzeBytes(bytes, extension, referenceId)
        } catch (e: Exception) {
            SignatureBrainResult.Failure(
                error = "Analysis error: ${e.message}",
                errorCode = SignatureErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Register a reference signature for future comparisons.
     *
     * @param file The reference signature image file
     * @param referenceId The ID to assign to this reference
     * @return True if registration succeeded
     */
    fun registerReference(file: File, referenceId: String): Boolean {
        return try {
            val bytes = file.readBytes()
            val hash = computeHash(bytes) ?: return false
            val dimensions = extractSignatureBounds(bytes)
            val strokeAnalysis = analyzeStrokes(bytes)
            val avgIntensity = calculateAverageIntensity(bytes)

            referenceSignatures[referenceId] = SignatureProfile(
                id = referenceId,
                hash = hash,
                strokeAnalysis = strokeAnalysis,
                dimensions = dimensions,
                averageIntensity = avgIntensity
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun analyzeBytes(bytes: ByteArray, extension: String, referenceId: String?): SignatureBrainResult {
        val signatureHash = computeHash(bytes)
            ?: return SignatureBrainResult.Failure(
                error = "Failed to compute signature hash",
                errorCode = SignatureErrorCode.PROCESSING_ERROR
            )

        val signatureType = detectSignatureType(bytes)
        val boundingBox = extractSignatureBounds(bytes)
        val strokeAnalysis = analyzeStrokes(bytes)
        val pixelAnalysis = analyzePixels(bytes, extension)
        val analysis = SignatureAnalysis(
            boundingBox = boundingBox,
            strokeAnalysis = strokeAnalysis,
            pressureConsistency = analyzePressureConsistency(bytes),
            inkConsistency = analyzeInkConsistency(bytes),
            backgroundConsistency = analyzeBackgroundConsistency(bytes),
            pixelAnalysis = pixelAnalysis
        )

        val anomalies = detectAnomalies(bytes, analysis, pixelAnalysis)

        // Compare with reference if provided
        val comparison = referenceId?.let { refId ->
            referenceSignatures[refId]?.let { ref ->
                compareWithReference(bytes, analysis, ref)
            }
        }

        val authenticityScore = calculateAuthenticityScore(analysis, anomalies, comparison)

        return SignatureBrainResult.Success(
            signatureHash = signatureHash,
            signatureType = signatureType,
            analysis = analysis,
            comparison = comparison,
            anomalies = anomalies,
            authenticityScore = authenticityScore
        )
    }

    /**
     * Compute SHA-512 hash.
     */
    private fun computeHash(bytes: ByteArray): String? {
        return try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            digest.update(bytes)
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Detect the type of signature.
     */
    private fun detectSignatureType(bytes: ByteArray): SignatureType {
        // Analyze image characteristics to determine signature type
        val pixelAnalysis = analyzePixelDistribution(bytes)

        return when {
            pixelAnalysis.isBinary && pixelAnalysis.hasUniformBackground -> {
                if (pixelAnalysis.hasSharpEdges) {
                    SignatureType.DIGITAL
                } else {
                    SignatureType.HANDWRITTEN
                }
            }
            pixelAnalysis.hasCompressionArtifacts && pixelAnalysis.hasUniformBackground -> {
                SignatureType.COPY_PASTE
            }
            pixelAnalysis.isStampLike -> SignatureType.STAMPED
            pixelAnalysis.isTypedFont -> SignatureType.TYPED
            else -> SignatureType.UNKNOWN
        }
    }

    /**
     * Pixel distribution analysis result.
     */
    private data class PixelDistributionAnalysis(
        val isBinary: Boolean,
        val hasUniformBackground: Boolean,
        val hasSharpEdges: Boolean,
        val hasCompressionArtifacts: Boolean,
        val isStampLike: Boolean,
        val isTypedFont: Boolean
    )

    /**
     * Analyze pixel distribution in the image.
     */
    private fun analyzePixelDistribution(bytes: ByteArray): PixelDistributionAnalysis {
        // Simplified analysis based on byte patterns
        val histogram = IntArray(256)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            histogram[value]++
        }

        // Check if binary (mostly black and white)
        val blackWhiteRatio = (histogram[0] + histogram[255]).toFloat() / bytes.size
        val isBinary = blackWhiteRatio > 0.7f

        // Check for uniform background
        val maxValue = histogram.max()
        val hasUniformBackground = maxValue > bytes.size * UNIFORM_BACKGROUND_THRESHOLD

        // Check for sharp edges (high contrast transitions)
        var sharpTransitions = 0
        for (i in 1 until bytes.size) {
            val diff = kotlin.math.abs((bytes[i].toInt() and 0xFF) - (bytes[i-1].toInt() and 0xFF))
            if (diff > 100) sharpTransitions++
        }
        val hasSharpEdges = sharpTransitions > bytes.size * 0.01

        // Check for compression artifacts (specific byte patterns)
        val hasCompressionArtifacts = detectJpegArtifacts(bytes)

        // Check for stamp-like pattern (circular or rectangular uniform ink)
        val isStampLike = histogram.count { it > bytes.size * 0.1 } <= 3

        // Check for typed font (very regular spacing and consistent ink)
        val isTypedFont = isRegularPattern(histogram)

        return PixelDistributionAnalysis(
            isBinary = isBinary,
            hasUniformBackground = hasUniformBackground,
            hasSharpEdges = hasSharpEdges,
            hasCompressionArtifacts = hasCompressionArtifacts,
            isStampLike = isStampLike,
            isTypedFont = isTypedFont
        )
    }

    /**
     * Detect JPEG compression artifacts.
     */
    private fun detectJpegArtifacts(bytes: ByteArray): Boolean {
        // Look for characteristic JPEG DCT patterns
        var blockPatterns = 0
        val blockSize = 8

        for (i in 0 until bytes.size - blockSize * 2) {
            var uniform = true
            val firstValue = bytes[i].toInt() and 0xFF
            for (j in 1 until blockSize) {
                if (kotlin.math.abs((bytes[i + j].toInt() and 0xFF) - firstValue) > 5) {
                    uniform = false
                    break
                }
            }
            if (uniform) blockPatterns++
        }

        return blockPatterns > bytes.size * 0.05
    }

    /**
     * Check for regular typed font pattern.
     */
    private fun isRegularPattern(histogram: IntArray): Boolean {
        val peaks = histogram.count { it > histogram.average() * 2 }
        return peaks in 2..5
    }

    /**
     * Extract signature bounding box.
     */
    private fun extractSignatureBounds(bytes: ByteArray): BoundingBox {
        // Simplified - return estimated bounds based on image size
        // Real implementation would analyze pixel values to find ink bounds
        val estimatedWidth = kotlin.math.sqrt(bytes.size.toDouble() * SIZE_TO_WIDTH_RATIO)
            .toInt()
            .coerceIn(MIN_ESTIMATED_WIDTH, MAX_ESTIMATED_WIDTH)
        val estimatedHeight = estimatedWidth / WIDTH_TO_HEIGHT_RATIO

        return BoundingBox(
            x = DEFAULT_MARGIN,
            y = DEFAULT_MARGIN,
            width = estimatedWidth,
            height = estimatedHeight
        )
    }

    /**
     * Analyze stroke characteristics.
     */
    private fun analyzeStrokes(bytes: ByteArray): StrokeAnalysis {
        // Simplified stroke analysis
        val strokeCount = estimateStrokeCount(bytes)
        val avgStrokeWidth = estimateAverageStrokeWidth(bytes)
        val strokeVariation = estimateStrokeVariation(bytes)

        return StrokeAnalysis(
            strokeCount = strokeCount,
            averageStrokeWidth = avgStrokeWidth,
            strokeVariation = strokeVariation,
            connectedness = estimateConnectedness(bytes),
            naturalFlow = strokeVariation > 0.1f && strokeVariation < 0.5f
        )
    }

    /**
     * Estimate number of strokes.
     */
    private fun estimateStrokeCount(bytes: ByteArray): Int {
        // Count transitions from light to dark
        var transitions = 0
        var inStroke = false

        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            val isDark = value < 128

            if (isDark && !inStroke) {
                transitions++
                inStroke = true
            } else if (!isDark) {
                inStroke = false
            }
        }

        return (transitions / 100).coerceIn(3, 50)
    }

    /**
     * Estimate average stroke width.
     */
    private fun estimateAverageStrokeWidth(bytes: ByteArray): Float {
        // Based on dark pixel density
        val darkPixels = bytes.count { (it.toInt() and 0xFF) < 128 }
        val ratio = darkPixels.toFloat() / bytes.size

        return (ratio * 10).coerceIn(1f, 5f)
    }

    /**
     * Estimate stroke variation.
     */
    private fun estimateStrokeVariation(bytes: ByteArray): Float {
        // Calculate variance in dark pixel intensity
        val darkPixels = bytes.filter { (it.toInt() and 0xFF) < 128 }
            .map { it.toInt() and 0xFF }

        if (darkPixels.isEmpty()) return 0f

        val mean = darkPixels.average()
        val variance = darkPixels.map { (it - mean) * (it - mean) }.average()

        return (variance / 1000).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Estimate stroke connectedness.
     */
    private fun estimateConnectedness(bytes: ByteArray): Float {
        // Higher values indicate more connected strokes
        var connected = 0
        var darkPixels = 0

        for (i in 1 until bytes.size) {
            val current = (bytes[i].toInt() and 0xFF) < 128
            val prev = (bytes[i-1].toInt() and 0xFF) < 128

            if (current) darkPixels++
            if (current && prev) connected++
        }

        return if (darkPixels > 0) {
            (connected.toFloat() / darkPixels).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * Analyze pixel characteristics.
     */
    private fun analyzePixels(bytes: ByteArray, extension: String): PixelAnalysis {
        val distribution = analyzePixelDistribution(bytes)

        return PixelAnalysis(
            uniformBackground = distribution.hasUniformBackground,
            edgeSharpness = if (distribution.hasSharpEdges) 0.9f else 0.5f,
            compressionArtifacts = distribution.hasCompressionArtifacts,
            copyPasteIndicators = detectCopyPasteIndicators(bytes)
        )
    }

    /**
     * Detect copy-paste indicators.
     */
    private fun detectCopyPasteIndicators(bytes: ByteArray): Boolean {
        // Look for signs of copy-paste:
        // 1. Perfect rectangular boundaries
        // 2. Inconsistent noise patterns
        // 3. Halo effects around signature

        val histogram = IntArray(256)
        for (byte in bytes) {
            histogram[byte.toInt() and 0xFF]++
        }

        // Copy-paste often has very specific color values (like 255 for white)
        val whitePixels = histogram[255]
        val nearWhitePixels = histogram.slice(250..254).sum()

        // If there are many pure white pixels but few near-white, it's suspicious
        return whitePixels > bytes.size * 0.3 && nearWhitePixels < whitePixels * 0.1
    }

    /**
     * Analyze pressure consistency (for handwritten signatures).
     */
    private fun analyzePressureConsistency(bytes: ByteArray): Float {
        val darkPixels = bytes.filter { (it.toInt() and 0xFF) < 128 }
            .map { it.toInt() and 0xFF }

        if (darkPixels.isEmpty()) return 0f

        val mean = darkPixels.average()
        val stdDev = kotlin.math.sqrt(darkPixels.map { (it - mean) * (it - mean) }.average())

        // Lower stdDev means more consistent pressure
        // Scale to 0-1 where 1 is very consistent
        return (1f - (stdDev / 50)).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Analyze ink consistency.
     */
    private fun analyzeInkConsistency(bytes: ByteArray): Boolean {
        val darkPixels = bytes.filter { (it.toInt() and 0xFF) < 128 }

        if (darkPixels.isEmpty()) return false

        // Check if ink color is consistent (not too much variation)
        val values = darkPixels.map { it.toInt() and 0xFF }
        val min = values.minOrNull() ?: 0
        val max = values.maxOrNull() ?: 255

        return (max - min) < 50
    }

    /**
     * Analyze background consistency.
     */
    private fun analyzeBackgroundConsistency(bytes: ByteArray): Boolean {
        val lightPixels = bytes.filter { (it.toInt() and 0xFF) >= 128 }

        if (lightPixels.isEmpty()) return false

        val values = lightPixels.map { it.toInt() and 0xFF }
        val min = values.minOrNull() ?: 128
        val max = values.maxOrNull() ?: 255

        return (max - min) < 30
    }

    /**
     * Calculate average pixel intensity.
     */
    private fun calculateAverageIntensity(bytes: ByteArray): Float {
        return bytes.map { it.toInt() and 0xFF }.average().toFloat() / 255f
    }

    /**
     * Detect anomalies in the signature.
     */
    private fun detectAnomalies(
        bytes: ByteArray,
        analysis: SignatureAnalysis,
        pixelAnalysis: PixelAnalysis
    ): List<SignatureAnomaly> {
        val anomalies = mutableListOf<SignatureAnomaly>()

        // Check for copy-paste
        if (pixelAnalysis.copyPasteIndicators) {
            anomalies.add(SignatureAnomaly(
                type = SignatureAnomalyType.COPY_PASTE_DETECTED,
                description = "Signature shows signs of being digitally pasted onto document",
                severity = AnomalySeverity.CRITICAL,
                location = analysis.boundingBox
            ))
        }

        // Check for digital overlay
        if (pixelAnalysis.edgeSharpness > EDGE_SHARPNESS_THRESHOLD && !analysis.inkConsistency) {
            anomalies.add(SignatureAnomaly(
                type = SignatureAnomalyType.DIGITAL_OVERLAY,
                description = "Signature edges are unnaturally sharp, suggesting digital creation",
                severity = AnomalySeverity.HIGH,
                location = analysis.boundingBox
            ))
        }

        // Check for pressure anomalies
        if (analysis.pressureConsistency > 0.95f && analysis.strokeAnalysis?.naturalFlow == false) {
            anomalies.add(SignatureAnomaly(
                type = SignatureAnomalyType.PRESSURE_ANOMALY,
                description = "Unnaturally consistent pressure suggesting mechanical or digital origin",
                severity = AnomalySeverity.MEDIUM,
                location = null
            ))
        }

        // Check for ink inconsistency
        if (!analysis.inkConsistency) {
            anomalies.add(SignatureAnomaly(
                type = SignatureAnomalyType.INK_INCONSISTENCY,
                description = "Ink color varies unusually across signature",
                severity = AnomalySeverity.LOW,
                location = null
            ))
        }

        // Check for background mismatch
        if (!analysis.backgroundConsistency) {
            anomalies.add(SignatureAnomaly(
                type = SignatureAnomalyType.BACKGROUND_MISMATCH,
                description = "Background is inconsistent, suggesting compositing",
                severity = AnomalySeverity.MEDIUM,
                location = null
            ))
        }

        // Check for compression artifacts
        if (pixelAnalysis.compressionArtifacts) {
            anomalies.add(SignatureAnomaly(
                type = SignatureAnomalyType.COMPRESSION_ARTIFACT,
                description = "JPEG compression artifacts detected around signature",
                severity = AnomalySeverity.LOW,
                location = null
            ))
        }

        return anomalies
    }

    /**
     * Compare signature with reference.
     */
    private fun compareWithReference(
        bytes: ByteArray,
        analysis: SignatureAnalysis,
        reference: SignatureProfile
    ): SignatureComparison {
        val matchingFeatures = mutableListOf<String>()
        val differingFeatures = mutableListOf<String>()

        // Compare dimensions
        val widthRatio = analysis.boundingBox.width.toFloat() / reference.dimensions.width
        val heightRatio = analysis.boundingBox.height.toFloat() / reference.dimensions.height

        if (widthRatio in 0.8f..1.2f && heightRatio in 0.8f..1.2f) {
            matchingFeatures.add("Proportions match")
        } else {
            differingFeatures.add("Proportions differ (${(widthRatio * 100).toInt()}% width, ${(heightRatio * 100).toInt()}% height)")
        }

        // Compare stroke analysis
        if (analysis.strokeAnalysis != null && reference.strokeAnalysis != null) {
            val strokeCountDiff = kotlin.math.abs(analysis.strokeAnalysis.strokeCount - reference.strokeAnalysis.strokeCount)
            if (strokeCountDiff <= 3) {
                matchingFeatures.add("Stroke count similar")
            } else {
                differingFeatures.add("Stroke count differs by $strokeCountDiff")
            }

            val widthDiff = kotlin.math.abs(analysis.strokeAnalysis.averageStrokeWidth - reference.strokeAnalysis.averageStrokeWidth)
            if (widthDiff < 1f) {
                matchingFeatures.add("Stroke width similar")
            } else {
                differingFeatures.add("Stroke width differs")
            }
        }

        // Compare intensity
        val currentIntensity = calculateAverageIntensity(bytes)
        val intensityDiff = kotlin.math.abs(currentIntensity - reference.averageIntensity)
        if (intensityDiff < 0.1f) {
            matchingFeatures.add("Ink intensity matches")
        } else {
            differingFeatures.add("Ink intensity differs")
        }

        // Calculate similarity score
        val totalFeatures = matchingFeatures.size + differingFeatures.size
        val similarityScore = if (totalFeatures > 0) {
            matchingFeatures.size.toFloat() / totalFeatures
        } else {
            0.5f
        }

        // Determine conclusion
        val conclusion = when {
            similarityScore >= 0.8f -> SignatureMatchConclusion.MATCH
            similarityScore >= 0.6f -> SignatureMatchConclusion.LIKELY_MATCH
            similarityScore >= 0.4f -> SignatureMatchConclusion.INCONCLUSIVE
            similarityScore >= 0.2f -> SignatureMatchConclusion.LIKELY_MISMATCH
            else -> SignatureMatchConclusion.MISMATCH
        }

        return SignatureComparison(
            referenceSignatureId = reference.id,
            similarityScore = similarityScore,
            matchingFeatures = matchingFeatures,
            differingFeatures = differingFeatures,
            conclusion = conclusion
        )
    }

    /**
     * Calculate overall authenticity score.
     */
    private fun calculateAuthenticityScore(
        analysis: SignatureAnalysis,
        anomalies: List<SignatureAnomaly>,
        comparison: SignatureComparison?
    ): Float {
        var score = 100f

        // Deduct for anomalies
        for (anomaly in anomalies) {
            score -= when (anomaly.severity) {
                AnomalySeverity.CRITICAL -> 30f
                AnomalySeverity.HIGH -> 20f
                AnomalySeverity.MEDIUM -> 10f
                AnomalySeverity.LOW -> 5f
                AnomalySeverity.INFO -> 2f
            }
        }

        // Adjust based on stroke analysis
        analysis.strokeAnalysis?.let { strokes ->
            if (!strokes.naturalFlow) score -= 10f
            if (strokes.strokeCount < 3) score -= 5f
        }

        // Adjust based on comparison
        comparison?.let { comp ->
            score = score * 0.7f + comp.similarityScore * 30f
        }

        return score.coerceIn(0f, 100f)
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: SignatureBrainResult): String {
        return when (result) {
            is SignatureBrainResult.Success -> buildSuccessJson(result)
            is SignatureBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: SignatureBrainResult.Success): String {
        val strokeAnalysisJson = result.analysis.strokeAnalysis?.let { s ->
            """{"strokeCount":${s.strokeCount},"averageStrokeWidth":${s.averageStrokeWidth},"strokeVariation":${s.strokeVariation},"connectedness":${s.connectedness},"naturalFlow":${s.naturalFlow}}"""
        } ?: "null"

        val comparisonJson = result.comparison?.let { c ->
            val matchingJson = c.matchingFeatures.joinToString(",") { "\"${escapeJson(it)}\"" }
            val differingJson = c.differingFeatures.joinToString(",") { "\"${escapeJson(it)}\"" }
            """{"referenceSignatureId":"${c.referenceSignatureId}","similarityScore":${c.similarityScore},"matchingFeatures":[$matchingJson],"differingFeatures":[$differingJson],"conclusion":"${c.conclusion}"}"""
        } ?: "null"

        val anomaliesJson = result.anomalies.joinToString(",") { a ->
            val locationJson = a.location?.let { l ->
                """{"x":${l.x},"y":${l.y},"width":${l.width},"height":${l.height}}"""
            } ?: "null"
            """{"type":"${a.type}","description":"${escapeJson(a.description)}","severity":"${a.severity}","location":$locationJson}"""
        }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "signatureId": "${result.signatureId}",
            "signatureHash": "${result.signatureHash}",
            "signatureType": "${result.signatureType}",
            "analysis": {
                "boundingBox": {
                    "x": ${result.analysis.boundingBox.x},
                    "y": ${result.analysis.boundingBox.y},
                    "width": ${result.analysis.boundingBox.width},
                    "height": ${result.analysis.boundingBox.height}
                },
                "strokeAnalysis": $strokeAnalysisJson,
                "pressureConsistency": ${result.analysis.pressureConsistency},
                "inkConsistency": ${result.analysis.inkConsistency},
                "backgroundConsistency": ${result.analysis.backgroundConsistency},
                "pixelAnalysis": {
                    "uniformBackground": ${result.analysis.pixelAnalysis.uniformBackground},
                    "edgeSharpness": ${result.analysis.pixelAnalysis.edgeSharpness},
                    "compressionArtifacts": ${result.analysis.pixelAnalysis.compressionArtifacts},
                    "copyPasteIndicators": ${result.analysis.pixelAnalysis.copyPasteIndicators}
                }
            },
            "comparison": $comparisonJson,
            "anomalies": [$anomaliesJson],
            "authenticityScore": ${result.authenticityScore}
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: SignatureBrainResult.Failure): String {
        return """
        {
            "success": false,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "error": "${escapeJson(result.error)}",
            "errorCode": "${result.errorCode}"
        }
        """.trimIndent()
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
