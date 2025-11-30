package com.verumdec.core.util

import java.io.*

/**
 * Utility class for safe file writing with atomic operations.
 * Ensures data integrity during write operations.
 */
object SafeWriteUtils {

    /**
     * Safely write content to file using atomic rename.
     * Writes to temp file first, then renames to target.
     */
    fun safeWriteText(file: File, content: String): Boolean {
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        return try {
            // Ensure parent directory exists
            file.parentFile?.mkdirs()
            
            // Write to temp file
            tempFile.writeText(content)
            
            // Delete existing target if present
            if (file.exists()) {
                file.delete()
            }
            
            // Atomic rename
            tempFile.renameTo(file)
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    /**
     * Safely write bytes to file using atomic rename.
     */
    fun safeWriteBytes(file: File, bytes: ByteArray): Boolean {
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        return try {
            file.parentFile?.mkdirs()
            tempFile.writeBytes(bytes)
            if (file.exists()) {
                file.delete()
            }
            tempFile.renameTo(file)
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    /**
     * Safely append content to file.
     */
    fun safeAppendText(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.appendText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Safely copy file with verification.
     */
    fun safeCopy(source: File, destination: File): Boolean {
        if (!source.exists()) return false
        
        val tempFile = File(destination.parentFile, "${destination.name}.tmp")
        return try {
            destination.parentFile?.mkdirs()
            
            source.inputStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Verify copy
            if (tempFile.length() != source.length()) {
                tempFile.delete()
                return false
            }
            
            if (destination.exists()) {
                destination.delete()
            }
            
            tempFile.renameTo(destination)
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    /**
     * Safely delete file or directory.
     */
    fun safeDelete(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create backup before writing.
     */
    fun writeWithBackup(file: File, content: String): Boolean {
        val backupFile = File(file.parentFile, "${file.name}.bak")
        return try {
            // Create backup of existing file
            if (file.exists()) {
                file.copyTo(backupFile, overwrite = true)
            }
            
            // Write new content
            if (safeWriteText(file, content)) {
                // Success - remove backup
                backupFile.delete()
                true
            } else {
                // Failed - restore backup
                if (backupFile.exists()) {
                    backupFile.renameTo(file)
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Restore backup on error
            if (backupFile.exists()) {
                backupFile.renameTo(file)
            }
            false
        }
    }

    /**
     * Safely create directory.
     */
    fun safeCreateDir(directory: File): Boolean {
        return try {
            directory.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if file can be safely written.
     */
    fun canWrite(file: File): Boolean {
        return try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            parent?.canWrite() ?: false
        } catch (e: Exception) {
            false
        }
    }
}
