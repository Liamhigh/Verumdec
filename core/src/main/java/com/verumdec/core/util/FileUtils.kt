package com.verumdec.core.util

import android.content.Context
import android.net.Uri
import java.io.*
import java.util.zip.ZipInputStream

/**
 * Utility class for file operations.
 * Provides local storage management for cases and evidence.
 */
object FileUtils {

    private const val CASES_DIR = "cases"
    private const val EVIDENCE_DIR = "evidence"
    private const val REPORTS_DIR = "reports"
    private const val TEMP_DIR = "temp"

    /**
     * Get the cases directory.
     */
    fun getCasesDir(context: Context): File {
        val dir = File(context.filesDir, CASES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get the evidence directory for a case.
     */
    fun getEvidenceDir(context: Context, caseId: String): File {
        val dir = File(getCasesDir(context), "$caseId/$EVIDENCE_DIR")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get the reports directory.
     */
    fun getReportsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), REPORTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get the temp directory.
     */
    fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, TEMP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copy content from URI to a local file.
     */
    fun copyFromUri(context: Context, uri: Uri, destination: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Read text content from a file.
     */
    fun readText(file: File): String {
        return file.readText()
    }

    /**
     * Write text content to a file.
     */
    fun writeText(file: File, content: String) {
        file.writeText(content)
    }

    /**
     * Read bytes from a file.
     */
    fun readBytes(file: File): ByteArray {
        return file.readBytes()
    }

    /**
     * Write bytes to a file.
     */
    fun writeBytes(file: File, bytes: ByteArray) {
        file.writeBytes(bytes)
    }

    /**
     * Get file extension.
     */
    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    /**
     * Get file name without extension.
     */
    fun getNameWithoutExtension(fileName: String): String {
        return fileName.substringBeforeLast('.')
    }

    /**
     * Delete a directory and all its contents.
     */
    fun deleteRecursively(file: File): Boolean {
        return file.deleteRecursively()
    }

    /**
     * Get file size in human-readable format.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Create a unique file name to avoid collisions.
     */
    fun createUniqueFileName(directory: File, baseName: String, extension: String): String {
        var counter = 0
        var fileName = "$baseName.$extension"
        while (File(directory, fileName).exists()) {
            counter++
            fileName = "${baseName}_$counter.$extension"
        }
        return fileName
    }

    /**
     * List all files in a directory with a specific extension.
     */
    fun listFiles(directory: File, extension: String): List<File> {
        return directory.listFiles { file ->
            file.isFile && file.extension.equals(extension, ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    /**
     * Extract ZIP file to directory.
     */
    fun extractZip(zipFile: File, destinationDir: File): Boolean {
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Ensure parent directories exist.
     */
    fun ensureParentDirs(file: File) {
        file.parentFile?.mkdirs()
    }

    /**
     * Check if file exists and is readable.
     */
    fun isReadable(file: File): Boolean {
        return file.exists() && file.canRead()
    }

    /**
     * Get all files in a case directory.
     */
    fun getCaseFiles(context: Context, caseId: String): List<File> {
        val caseDir = File(getCasesDir(context), caseId)
        return caseDir.walkTopDown().filter { it.isFile }.toList()
    }
}
