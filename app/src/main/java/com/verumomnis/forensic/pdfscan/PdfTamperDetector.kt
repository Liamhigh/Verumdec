package com.verumomnis.forensic.pdfscan

import android.content.Context
import java.io.File

/**
 * PDF Tamper Detector - Analyzes PDF files for tampering indicators.
 * Checks XRef structure, metadata, embedded objects, and incremental updates.
 */
object PdfTamperDetector {

    data class PdfTamperReport(
        val hasIncrementalUpdates: Boolean,
        val xrefIssues: List<String>,
        val metadataIssues: List<String>,
        val objectIssues: List<String>,
        val embeddedObjects: List<String>,
        val flags: List<String>
    )

    /**
     * Perform comprehensive PDF tamper detection.
     */
    fun analyse(context: Context, pdfFile: File): PdfTamperReport {
        val content = pdfFile.readBytes()
        val textContent = String(content, Charsets.ISO_8859_1)

        val hasIncrementalUpdates = detectIncrementalUpdates(textContent)
        val xrefIssues = analyseXRef(textContent)
        val metadataIssues = analyseMetadata(textContent)
        val objectIssues = analyseObjects(textContent)
        val embeddedObjects = detectEmbeddedObjects(textContent)

        val flags = mutableListOf<String>()

        if (hasIncrementalUpdates) {
            flags.add("PDF has incremental updates - document was modified after creation")
        }

        flags.addAll(xrefIssues)
        flags.addAll(metadataIssues)
        flags.addAll(objectIssues)

        if (embeddedObjects.isNotEmpty()) {
            flags.add("PDF contains ${embeddedObjects.size} embedded object(s)")
        }

        return PdfTamperReport(
            hasIncrementalUpdates = hasIncrementalUpdates,
            xrefIssues = xrefIssues,
            metadataIssues = metadataIssues,
            objectIssues = objectIssues,
            embeddedObjects = embeddedObjects,
            flags = flags
        )
    }

    /**
     * Detect incremental updates in PDF.
     */
    private fun detectIncrementalUpdates(content: String): Boolean {
        // Count %%EOF markers - multiple indicates incremental saves
        val eofCount = Regex("%%EOF").findAll(content).count()
        return eofCount > 1
    }

    /**
     * Analyze XRef table for anomalies.
     */
    private fun analyseXRef(content: String): List<String> {
        val issues = mutableListOf<String>()

        // Check for multiple xref tables
        val xrefCount = Regex("xref").findAll(content).count()
        if (xrefCount > 1) {
            issues.add("Multiple XRef tables detected ($xrefCount) - indicates document modification")
        }

        // Check for xref stream (PDF 1.5+)
        if (content.contains("/XRef") && content.contains("xref")) {
            issues.add("Mixed XRef formats (table + stream) - unusual structure")
        }

        return issues
    }

    /**
     * Analyze metadata for tampering indicators.
     */
    private fun analyseMetadata(content: String): List<String> {
        val issues = mutableListOf<String>()

        // Check for modification date
        val creationMatch = Regex("/CreationDate\\s*\\(([^)]+)\\)").find(content)
        val modMatch = Regex("/ModDate\\s*\\(([^)]+)\\)").find(content)

        if (creationMatch != null && modMatch != null) {
            if (creationMatch.groupValues[1] != modMatch.groupValues[1]) {
                issues.add("Creation and modification dates differ - document was edited")
            }
        }

        // Check for producer/creator mismatch
        val producer = Regex("/Producer\\s*\\(([^)]+)\\)").find(content)?.groupValues?.get(1) ?: ""
        val creator = Regex("/Creator\\s*\\(([^)]+)\\)").find(content)?.groupValues?.get(1) ?: ""

        if (producer.isNotEmpty() && creator.isNotEmpty() && producer != creator) {
            issues.add("Producer/Creator mismatch: document may have been re-saved by different software")
        }

        return issues
    }

    /**
     * Analyze PDF objects for anomalies.
     */
    private fun analyseObjects(content: String): List<String> {
        val issues = mutableListOf<String>()

        // Check for JavaScript
        if (content.contains("/JavaScript") || content.contains("/JS")) {
            issues.add("PDF contains JavaScript - potential security risk")
        }

        // Check for embedded files
        if (content.contains("/EmbeddedFile") || content.contains("/Filespec")) {
            issues.add("PDF contains embedded files")
        }

        // Check for form fields (potential data injection)
        if (content.contains("/AcroForm")) {
            issues.add("PDF contains form fields - data may be editable")
        }

        // Check for digital signatures
        if (content.contains("/Sig") && content.contains("/ByteRange")) {
            issues.add("PDF has digital signature - verify signature validity separately")
        }

        return issues
    }

    /**
     * Detect embedded objects in PDF.
     */
    private fun detectEmbeddedObjects(content: String): List<String> {
        val objects = mutableListOf<String>()

        // Detect embedded images
        val imageCount = Regex("/Subtype\\s*/Image").findAll(content).count()
        if (imageCount > 0) {
            objects.add("$imageCount embedded image(s)")
        }

        // Detect embedded fonts
        val fontCount = Regex("/Type\\s*/Font").findAll(content).count()
        if (fontCount > 0) {
            objects.add("$fontCount embedded font(s)")
        }

        // Detect embedded streams
        val streamCount = Regex("stream\\s").findAll(content).count()
        if (streamCount > 10) {
            objects.add("$streamCount data stream(s)")
        }

        return objects
    }
}
