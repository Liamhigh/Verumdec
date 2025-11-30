package com.verumdec.core.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility class for cryptographic hash operations.
 * Used for document sealing and integrity verification.
 */
object HashUtils {

    /**
     * Calculate SHA-512 hash of a string.
     */
    fun sha512(content: String): String {
        return sha512(content.toByteArray())
    }

    /**
     * Calculate SHA-512 hash of byte array.
     */
    fun sha512(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate SHA-512 hash of a file.
     */
    fun sha512(file: File): String {
        return sha512(file.readBytes())
    }

    /**
     * Calculate SHA-512 hash of an input stream.
     */
    fun sha512(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate SHA-256 hash of a string.
     */
    fun sha256(content: String): String {
        return sha256(content.toByteArray())
    }

    /**
     * Calculate SHA-256 hash of byte array.
     */
    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get truncated hash for display (first 32 characters).
     */
    fun truncatedSha512(content: String, length: Int = 32): String {
        return sha512(content).take(length)
    }

    /**
     * Verify content against expected hash.
     */
    fun verifySha512(content: String, expectedHash: String): Boolean {
        return sha512(content) == expectedHash
    }

    /**
     * Generate a unique hash-based ID.
     */
    fun generateHashId(prefix: String = ""): String {
        val timestamp = System.currentTimeMillis()
        val random = (Math.random() * 1000000).toLong()
        val combined = "$prefix$timestamp$random"
        return sha256(combined).take(16)
    }
}
