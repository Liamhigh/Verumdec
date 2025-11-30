package com.verumdec.report.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File utilities for managing sealed PDF reports.
 * 
 * Provides methods for saving, loading, and managing sealed forensic reports
 * with proper Android storage handling for Android 12+ compatibility.
 */
object FileUtils {

    private const val REPORTS_DIRECTORY = "verumdec_reports"
    private const val SEALED_REPORTS_SUBDIRECTORY = "sealed"
    private const val PDF_EXTENSION = ".pdf"
    private const val HASH_FILE_EXTENSION = ".sha512"
    private const val DATE_FORMAT = "yyyyMMdd_HHmmss"

    /** SHA-512 hash length in hexadecimal characters (512 bits / 4 bits per hex char = 128) */
    private const val SHA512_HEX_LENGTH = 128

    /**
     * Save a sealed PDF report to local storage.
     *
     * @param context Android context
     * @param pdfBytes The PDF file bytes to save
     * @param caseId The case identifier
     * @param sha512Hash The SHA-512 hash of the document
     * @return The File object pointing to the saved PDF, or null if saving fails
     */
    fun saveSealedReport(
        context: Context,
        pdfBytes: ByteArray,
        caseId: String,
        sha512Hash: String
    ): File? {
        return try {
            val reportsDir = getSealedReportsDirectory(context)
            if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                return null
            }

            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
            val sanitizedCaseId = sanitizeFileName(caseId)
            val fileName = "sealed_${sanitizedCaseId}_$timestamp$PDF_EXTENSION"

            val pdfFile = File(reportsDir, fileName)

            FileOutputStream(pdfFile).use { fos ->
                fos.write(pdfBytes)
                fos.flush()
            }

            // Save hash file alongside the PDF for verification
            saveHashFile(reportsDir, fileName, sha512Hash)

            pdfFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save the SHA-512 hash to a companion file.
     */
    private fun saveHashFile(
        directory: File,
        pdfFileName: String,
        sha512Hash: String
    ) {
        try {
            val hashFileName = pdfFileName.replace(PDF_EXTENSION, HASH_FILE_EXTENSION)
            val hashFile = File(directory, hashFileName)

            hashFile.writeText(sha512Hash)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Get the directory for sealed reports.
     * Uses app-specific external storage for Android 12+ compatibility.
     * Falls back to internal storage if external is unavailable.
     *
     * @param context Android context
     * @return File object representing the sealed reports directory
     */
    fun getSealedReportsDirectory(context: Context): File {
        // Use app-specific external storage (no permissions needed on Android 12+)
        // Falls back to internal storage if external is unavailable
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir

        return File(baseDir, "$REPORTS_DIRECTORY/$SEALED_REPORTS_SUBDIRECTORY")
    }

    /**
     * Get all sealed reports in the reports directory.
     *
     * @param context Android context
     * @return List of PDF files in the sealed reports directory
     */
    fun getSealedReports(context: Context): List<File> {
        val reportsDir = getSealedReportsDirectory(context)
        return if (reportsDir.exists()) {
            reportsDir.listFiles { file ->
                file.isFile && file.name.endsWith(PDF_EXTENSION)
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Verify a sealed report's integrity by checking its hash file.
     *
     * Note: The stored hash represents the document content before the QR verification
     * seal was added. For accurate verification, one would need to parse the PDF,
     * extract the hash from the QR code, and compare it against the stored hash file.
     * This simplified verification checks if the hash file exists and contains valid data.
     * Full cryptographic verification requires QR code scanning from the PDF.
     *
     * @param context Android context (reserved for future use)
     * @param pdfFile The PDF file to verify
     * @return True if the hash file exists and contains valid SHA-512 hash, false otherwise
     */
    @Suppress("UNUSED_PARAMETER")
    fun verifySealedReport(context: Context, pdfFile: File): Boolean {
        return try {
            val hashFileName = pdfFile.name.replace(PDF_EXTENSION, HASH_FILE_EXTENSION)
            val hashFile = File(pdfFile.parentFile, hashFileName)

            if (!hashFile.exists()) {
                return false
            }

            // Verify the hash file is readable and contains valid hash data
            val storedHash = hashFile.readText().trim()
            isValidSHA512Hash(storedHash)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Compute SHA-512 hash of a file.
     *
     * @param file The file to hash
     * @return Hex string of the SHA-512 hash
     */
    fun computeSHA512(file: File): String {
        val digest = MessageDigest.getInstance("SHA-512")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return bytesToHex(digest.digest())
    }

    /**
     * Compute SHA-512 hash of byte array.
     *
     * @param data The byte array to hash
     * @return Hex string of the SHA-512 hash
     */
    fun computeSHA512(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(data)
        return bytesToHex(hashBytes)
    }

    /**
     * Validate that a string is a valid SHA-512 hash format.
     *
     * @param hash The hash string to validate
     * @return True if the string is a valid 128-character hexadecimal SHA-512 hash
     */
    fun isValidSHA512Hash(hash: String): Boolean {
        return hash.length == SHA512_HEX_LENGTH && 
            hash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Convert bytes to hexadecimal string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Sanitize a string for use as a filename.
     *
     * @param input The input string
     * @return Sanitized string safe for use in filenames
     */
    fun sanitizeFileName(input: String): String {
        return input
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .take(50)
            .trim('_')
    }

    /**
     * Delete a sealed report and its hash file.
     *
     * @param pdfFile The PDF file to delete
     * @return True if both files were deleted successfully
     */
    fun deleteSealedReport(pdfFile: File): Boolean {
        return try {
            val hashFileName = pdfFile.name.replace(PDF_EXTENSION, HASH_FILE_EXTENSION)
            val hashFile = File(pdfFile.parentFile, hashFileName)

            val pdfDeleted = pdfFile.delete()
            val hashDeleted = if (hashFile.exists()) hashFile.delete() else true

            pdfDeleted && hashDeleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get the storage size of all sealed reports.
     *
     * @param context Android context
     * @return Total size in bytes
     */
    fun getSealedReportsSize(context: Context): Long {
        val reportsDir = getSealedReportsDirectory(context)
        return if (reportsDir.exists()) {
            reportsDir.listFiles()?.sumOf { it.length() } ?: 0L
        } else {
            0L
        }
    }

    /**
     * Format file size for display.
     *
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
