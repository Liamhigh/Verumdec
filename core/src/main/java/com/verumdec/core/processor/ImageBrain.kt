package com.verumdec.core.processor

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * ImageBrain - Detect edits, inconsistencies, compression anomalies
 *
 * Analyzes image files to detect potential manipulations, editing artifacts,
 * compression anomalies, and other integrity issues.
 *
 * Operates fully offline without external dependencies.
 */
class ImageBrain {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp"
        )

        // JPEG quality estimation thresholds
        private const val HIGH_QUALITY_THRESHOLD = 85
        private const val MEDIUM_QUALITY_THRESHOLD = 60
    }

    /**
     * Process an image file and analyze for anomalies.
     *
     * @param file The image file to process
     * @return ImageBrainResult containing analysis data or error
     */
    fun process(file: File): ImageBrainResult {
        return try {
            if (!file.exists()) {
                return ImageBrainResult.Failure(
                    error = "File not found: ${file.absolutePath}",
                    errorCode = ImageErrorCode.FILE_NOT_FOUND
                )
            }

            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return ImageBrainResult.Failure(
                    error = "Unsupported image format: $extension",
                    errorCode = ImageErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = file.readBytes()
            processBytes(bytes, extension)
        } catch (e: Exception) {
            ImageBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = ImageErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Process an image from an input stream.
     *
     * @param inputStream The input stream to read from
     * @param fileName The original filename for extension detection
     * @return ImageBrainResult containing analysis data or error
     */
    fun process(inputStream: InputStream, fileName: String): ImageBrainResult {
        return try {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return ImageBrainResult.Failure(
                    error = "Unsupported image format: $extension",
                    errorCode = ImageErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = inputStream.readBytes()
            processBytes(bytes, extension)
        } catch (e: Exception) {
            ImageBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = ImageErrorCode.PROCESSING_ERROR
            )
        }
    }

    private fun processBytes(bytes: ByteArray, extension: String): ImageBrainResult {
        val imageHash = computeHash(bytes)
            ?: return ImageBrainResult.Failure(
                error = "Failed to compute image hash",
                errorCode = ImageErrorCode.ANALYSIS_FAILED
            )

        val dimensions = extractDimensions(bytes, extension)
            ?: return ImageBrainResult.Failure(
                error = "Failed to extract image dimensions",
                errorCode = ImageErrorCode.CORRUPTED_IMAGE
            )

        val format = detectFormat(bytes, extension)
        val compressionAnalysis = analyzeCompression(bytes, extension)
        val editDetection = detectEdits(bytes, extension)
        val exifData = extractExifData(bytes, extension)
        val anomalies = detectAnomalies(bytes, extension, exifData, compressionAnalysis)

        // Calculate integrity score based on anomalies
        val integrityScore = calculateIntegrityScore(anomalies, editDetection, compressionAnalysis)

        return ImageBrainResult.Success(
            imageHash = imageHash,
            dimensions = dimensions,
            format = format,
            compressionAnalysis = compressionAnalysis,
            editDetection = editDetection,
            exifData = exifData,
            anomalies = anomalies,
            integrityScore = integrityScore
        )
    }

    /**
     * Compute SHA-512 hash of image bytes.
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
     * Extract image dimensions from file bytes.
     */
    private fun extractDimensions(bytes: ByteArray, extension: String): ImageDimensions? {
        return try {
            when (extension) {
                "jpg", "jpeg" -> extractJpegDimensions(bytes)
                "png" -> extractPngDimensions(bytes)
                "gif" -> extractGifDimensions(bytes)
                "bmp" -> extractBmpDimensions(bytes)
                else -> ImageDimensions(0, 0, 24) // Unknown
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJpegDimensions(bytes: ByteArray): ImageDimensions? {
        var i = 2 // Skip SOI marker
        while (i < bytes.size - 4) {
            if (bytes[i] == 0xFF.toByte()) {
                val marker = bytes[i + 1].toInt() and 0xFF
                // SOF markers contain dimensions
                if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                    val height = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
                    val width = ((bytes[i + 7].toInt() and 0xFF) shl 8) or (bytes[i + 8].toInt() and 0xFF)
                    val components = bytes[i + 9].toInt() and 0xFF
                    return ImageDimensions(width, height, components * 8)
                }
                if (marker == 0xD9 || marker == 0xDA) break // EOI or SOS
                val length = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                i += length + 2
            } else {
                i++
            }
        }
        return null
    }

    private fun extractPngDimensions(bytes: ByteArray): ImageDimensions? {
        // PNG IHDR chunk starts at byte 16
        if (bytes.size < 24) return null
        val width = ((bytes[16].toInt() and 0xFF) shl 24) or
                ((bytes[17].toInt() and 0xFF) shl 16) or
                ((bytes[18].toInt() and 0xFF) shl 8) or
                (bytes[19].toInt() and 0xFF)
        val height = ((bytes[20].toInt() and 0xFF) shl 24) or
                ((bytes[21].toInt() and 0xFF) shl 16) or
                ((bytes[22].toInt() and 0xFF) shl 8) or
                (bytes[23].toInt() and 0xFF)
        val bitDepth = bytes[24].toInt() and 0xFF
        return ImageDimensions(width, height, bitDepth)
    }

    private fun extractGifDimensions(bytes: ByteArray): ImageDimensions? {
        if (bytes.size < 10) return null
        val width = ((bytes[7].toInt() and 0xFF) shl 8) or (bytes[6].toInt() and 0xFF)
        val height = ((bytes[9].toInt() and 0xFF) shl 8) or (bytes[8].toInt() and 0xFF)
        return ImageDimensions(width, height, 8)
    }

    private fun extractBmpDimensions(bytes: ByteArray): ImageDimensions? {
        if (bytes.size < 26) return null
        val width = ((bytes[21].toInt() and 0xFF) shl 24) or
                ((bytes[20].toInt() and 0xFF) shl 16) or
                ((bytes[19].toInt() and 0xFF) shl 8) or
                (bytes[18].toInt() and 0xFF)
        val height = ((bytes[25].toInt() and 0xFF) shl 24) or
                ((bytes[24].toInt() and 0xFF) shl 16) or
                ((bytes[23].toInt() and 0xFF) shl 8) or
                (bytes[22].toInt() and 0xFF)
        val bitDepth = ((bytes[29].toInt() and 0xFF) shl 8) or (bytes[28].toInt() and 0xFF)
        return ImageDimensions(width, kotlin.math.abs(height), bitDepth)
    }

    /**
     * Detect actual image format from magic bytes.
     */
    private fun detectFormat(bytes: ByteArray, extension: String): String {
        if (bytes.size < 4) return extension.uppercase()

        return when {
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "JPEG"
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "PNG"
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x46.toByte() -> "GIF"
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> "BMP"
            bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte() -> "TIFF (Little Endian)"
            bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte() -> "TIFF (Big Endian)"
            bytes.size >= 12 && bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                    bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> "WEBP"
            else -> extension.uppercase()
        }
    }

    /**
     * Analyze image compression characteristics.
     */
    private fun analyzeCompression(bytes: ByteArray, extension: String): CompressionAnalysis {
        val compressionType = when (extension) {
            "jpg", "jpeg" -> "JPEG (Lossy DCT)"
            "png" -> "PNG (Lossless DEFLATE)"
            "gif" -> "GIF (LZW)"
            "bmp" -> "Uncompressed"
            "tiff", "tif" -> "TIFF (Various)"
            "webp" -> "WEBP (VP8)"
            else -> "Unknown"
        }

        val qualityEstimate = estimateJpegQuality(bytes, extension)
        val recompressionDetected = detectRecompression(bytes, extension)
        val artifacts = detectCompressionArtifacts(bytes, extension)

        return CompressionAnalysis(
            compressionType = compressionType,
            qualityEstimate = qualityEstimate,
            recompressionDetected = recompressionDetected,
            compressionArtifacts = artifacts
        )
    }

    /**
     * Estimate JPEG quality from quantization tables.
     */
    private fun estimateJpegQuality(bytes: ByteArray, extension: String): Float {
        if (extension !in listOf("jpg", "jpeg")) return 100f

        // Look for DQT marker (0xFF, 0xDB) and analyze quantization values
        var i = 2
        while (i < bytes.size - 4) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xDB.toByte()) {
                // Found quantization table
                val length = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                if (i + 4 + 64 <= bytes.size) {
                    // Analyze first quantization table values
                    var sum = 0
                    for (j in 0 until 64.coerceAtMost(length - 2)) {
                        sum += bytes[i + 5 + j].toInt() and 0xFF
                    }
                    val avgQuantization = sum / 64.0f

                    // Estimate quality inversely from quantization values
                    return when {
                        avgQuantization < 5 -> 95f
                        avgQuantization < 10 -> 90f
                        avgQuantization < 20 -> 80f
                        avgQuantization < 40 -> 70f
                        avgQuantization < 60 -> 60f
                        avgQuantization < 80 -> 50f
                        else -> 40f
                    }
                }
                break
            }
            i++
        }
        return 75f // Default estimate
    }

    /**
     * Detect potential recompression artifacts.
     */
    private fun detectRecompression(bytes: ByteArray, extension: String): Boolean {
        if (extension !in listOf("jpg", "jpeg")) return false

        // Count quantization tables - multiple tables might indicate recompression
        var dqtCount = 0
        for (i in 0 until bytes.size - 1) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xDB.toByte()) {
                dqtCount++
            }
        }

        // Multiple restarts or unusual patterns can indicate recompression
        return dqtCount > 2
    }

    /**
     * Detect compression artifacts in the image.
     */
    private fun detectCompressionArtifacts(bytes: ByteArray, extension: String): List<String> {
        val artifacts = mutableListOf<String>()

        if (extension in listOf("jpg", "jpeg")) {
            val quality = estimateJpegQuality(bytes, extension)
            if (quality < MEDIUM_QUALITY_THRESHOLD) {
                artifacts.add("Low quality compression detected (estimated ${quality.toInt()}%)")
            }
            if (detectRecompression(bytes, extension)) {
                artifacts.add("Possible recompression detected")
            }
        }

        return artifacts
    }

    /**
     * Detect potential image edits.
     */
    private fun detectEdits(bytes: ByteArray, extension: String): EditDetection {
        val editRegions = mutableListOf<EditRegion>()
        var cloneDetected = false
        var spliceDetected = false
        var filterApplied = false
        var resizeDetected = false

        // Check for software markers in JPEG/PNG that indicate editing
        val content = String(bytes, Charsets.ISO_8859_1)

        // Common editing software signatures
        val editingSoftware = listOf(
            "Photoshop", "GIMP", "Paint.NET", "Pixlr", "Canva",
            "Lightroom", "Affinity", "Snapseed", "VSCO", "PicsArt"
        )
        for (software in editingSoftware) {
            if (content.contains(software, ignoreCase = true)) {
                filterApplied = true
                break
            }
        }

        // Check for resize indicators
        if (content.contains("resize", ignoreCase = true) ||
            content.contains("scaled", ignoreCase = true)) {
            resizeDetected = true
        }

        // Basic statistical analysis for clone/splice detection
        // (Simplified - real implementation would use DCT analysis)
        val confidence = when {
            filterApplied && resizeDetected -> 0.7f
            filterApplied || resizeDetected -> 0.5f
            else -> 0.9f
        }

        return EditDetection(
            edited = filterApplied || resizeDetected,
            editRegions = editRegions,
            cloneDetected = cloneDetected,
            spliceDetected = spliceDetected,
            filterApplied = filterApplied,
            resizeDetected = resizeDetected,
            confidence = confidence
        )
    }

    /**
     * Extract EXIF data from image.
     */
    private fun extractExifData(bytes: ByteArray, extension: String): Map<String, String> {
        val exifData = mutableMapOf<String, String>()

        if (extension !in listOf("jpg", "jpeg", "tiff", "tif")) {
            return exifData
        }

        // Look for EXIF marker (APP1)
        for (i in 0 until bytes.size - 6) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xE1.toByte()) {
                // Check for "Exif" identifier
                if (i + 10 < bytes.size) {
                    val identifier = String(bytes.sliceArray(i + 4 until i + 8), Charsets.US_ASCII)
                    if (identifier == "Exif") {
                        exifData["hasExif"] = "true"
                        // Extract basic EXIF info from readable strings
                        extractExifStrings(bytes, i, exifData)
                        break
                    }
                }
            }
        }

        if (!exifData.containsKey("hasExif")) {
            exifData["hasExif"] = "false"
        }

        return exifData
    }

    private fun extractExifStrings(bytes: ByteArray, exifStart: Int, exifData: MutableMap<String, String>) {
        // Extract readable strings from EXIF segment
        val exifLength = ((bytes[exifStart + 2].toInt() and 0xFF) shl 8) or
                (bytes[exifStart + 3].toInt() and 0xFF)
        val exifEnd = (exifStart + exifLength).coerceAtMost(bytes.size)

        val exifContent = String(bytes.sliceArray(exifStart until exifEnd), Charsets.ISO_8859_1)

        // Look for common EXIF fields
        val makeMatch = Regex("(?i)make[^a-z]*([A-Za-z0-9 ]+)").find(exifContent)
        if (makeMatch != null) {
            exifData["Make"] = makeMatch.groupValues[1].trim()
        }

        val modelMatch = Regex("(?i)model[^a-z]*([A-Za-z0-9 ]+)").find(exifContent)
        if (modelMatch != null) {
            exifData["Model"] = modelMatch.groupValues[1].trim()
        }

        // Check for GPS data presence
        if (exifContent.contains("GPS", ignoreCase = true)) {
            exifData["hasGPS"] = "true"
        }
    }

    /**
     * Detect anomalies in the image.
     */
    private fun detectAnomalies(
        bytes: ByteArray,
        extension: String,
        exifData: Map<String, String>,
        compressionAnalysis: CompressionAnalysis
    ): List<ImageAnomaly> {
        val anomalies = mutableListOf<ImageAnomaly>()

        // Check for format mismatch
        val detectedFormat = detectFormat(bytes, extension)
        if (!detectedFormat.equals(extension, ignoreCase = true) &&
            !detectedFormat.startsWith(extension.uppercase())) {
            anomalies.add(ImageAnomaly(
                type = ImageAnomalyType.METADATA_INCONSISTENCY,
                description = "File extension ($extension) does not match detected format ($detectedFormat)",
                severity = AnomalySeverity.HIGH
            ))
        }

        // Check for missing EXIF in JPEG
        if (extension in listOf("jpg", "jpeg") && exifData["hasExif"] != "true") {
            anomalies.add(ImageAnomaly(
                type = ImageAnomalyType.EXIF_MANIPULATION,
                description = "EXIF data is missing - may have been stripped",
                severity = AnomalySeverity.MEDIUM
            ))
        }

        // Check compression artifacts
        if (compressionAnalysis.recompressionDetected) {
            anomalies.add(ImageAnomaly(
                type = ImageAnomalyType.COMPRESSION_ARTIFACT,
                description = "Image appears to have been recompressed multiple times",
                severity = AnomalySeverity.MEDIUM
            ))
        }

        // Check for very low quality
        if (compressionAnalysis.qualityEstimate < 50f) {
            anomalies.add(ImageAnomaly(
                type = ImageAnomalyType.COMPRESSION_ARTIFACT,
                description = "Very low quality compression detected (${compressionAnalysis.qualityEstimate.toInt()}%)",
                severity = AnomalySeverity.LOW
            ))
        }

        return anomalies
    }

    /**
     * Calculate overall integrity score.
     */
    private fun calculateIntegrityScore(
        anomalies: List<ImageAnomaly>,
        editDetection: EditDetection,
        compressionAnalysis: CompressionAnalysis
    ): Float {
        var score = 100f

        // Deduct for anomalies
        for (anomaly in anomalies) {
            score -= when (anomaly.severity) {
                AnomalySeverity.CRITICAL -> 25f
                AnomalySeverity.HIGH -> 15f
                AnomalySeverity.MEDIUM -> 10f
                AnomalySeverity.LOW -> 5f
                AnomalySeverity.INFO -> 2f
            }
        }

        // Deduct for edits detected
        if (editDetection.edited) score -= 10f
        if (editDetection.cloneDetected) score -= 20f
        if (editDetection.spliceDetected) score -= 20f
        if (editDetection.resizeDetected) score -= 5f

        // Deduct for recompression
        if (compressionAnalysis.recompressionDetected) score -= 10f

        return score.coerceIn(0f, 100f)
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: ImageBrainResult): String {
        return when (result) {
            is ImageBrainResult.Success -> buildSuccessJson(result)
            is ImageBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: ImageBrainResult.Success): String {
        val exifJson = result.exifData.entries.joinToString(",") {
            "\"${escapeJson(it.key)}\":\"${escapeJson(it.value)}\""
        }
        val anomaliesJson = result.anomalies.joinToString(",") { anomaly ->
            """{"type":"${anomaly.type}","description":"${escapeJson(anomaly.description)}","severity":"${anomaly.severity}"}"""
        }
        val artifactsJson = result.compressionAnalysis.compressionArtifacts.joinToString(",") {
            "\"${escapeJson(it)}\""
        }
        val editRegionsJson = result.editDetection.editRegions.joinToString(",") { region ->
            """{"x":${region.x},"y":${region.y},"width":${region.width},"height":${region.height},"type":"${region.type}","confidence":${region.confidence}}"""
        }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "imageId": "${result.imageId}",
            "imageHash": "${result.imageHash}",
            "dimensions": {
                "width": ${result.dimensions.width},
                "height": ${result.dimensions.height},
                "bitDepth": ${result.dimensions.bitDepth}
            },
            "format": "${result.format}",
            "compressionAnalysis": {
                "compressionType": "${result.compressionAnalysis.compressionType}",
                "qualityEstimate": ${result.compressionAnalysis.qualityEstimate},
                "recompressionDetected": ${result.compressionAnalysis.recompressionDetected},
                "compressionArtifacts": [$artifactsJson]
            },
            "editDetection": {
                "edited": ${result.editDetection.edited},
                "editRegions": [$editRegionsJson],
                "cloneDetected": ${result.editDetection.cloneDetected},
                "spliceDetected": ${result.editDetection.spliceDetected},
                "filterApplied": ${result.editDetection.filterApplied},
                "resizeDetected": ${result.editDetection.resizeDetected},
                "confidence": ${result.editDetection.confidence}
            },
            "exifData": {$exifJson},
            "anomalies": [$anomaliesJson],
            "integrityScore": ${result.integrityScore}
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: ImageBrainResult.Failure): String {
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
