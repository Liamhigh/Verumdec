package com.verumdec.core

import java.security.MessageDigest

/**
 * Utility functions for cryptographic hash operations.
 * Used for document sealing and integrity verification.
 */
object HashUtils {
    
    /**
     * Calculate SHA-512 hash of a string.
     * This is the primary hash algorithm used for document sealing.
     */
    fun sha512(content: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashBytes)
    }
    
    /**
     * Calculate SHA-512 hash of a byte array.
     */
    fun sha512(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(content)
        return bytesToHex(hashBytes)
    }
    
    /**
     * Calculate SHA-256 hash of a string.
     */
    fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashBytes)
    }
    
    /**
     * Calculate SHA-256 hash of a byte array.
     */
    fun sha256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content)
        return bytesToHex(hashBytes)
    }
    
    /**
     * Calculate MD5 hash of a string.
     * Note: MD5 is not cryptographically secure, use for checksums only.
     */
    fun md5(content: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashBytes)
    }
    
    /**
     * Convert byte array to hexadecimal string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify a SHA-512 hash matches content.
     */
    fun verifySha512(content: String, expectedHash: String): Boolean {
        val actualHash = sha512(content)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
    
    /**
     * Verify a SHA-256 hash matches content.
     */
    fun verifySha256(content: String, expectedHash: String): Boolean {
        val actualHash = sha256(content)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
    
    /**
     * Get truncated hash for display purposes.
     */
    fun truncateHash(hash: String, length: Int = 16): String {
        return if (hash.length > length) {
            "${hash.take(length)}..."
        } else {
            hash
        }
    }
    
    /**
     * Format hash with separator for readability.
     */
    fun formatHash(hash: String, groupSize: Int = 8, separator: String = " "): String {
        return hash.chunked(groupSize).joinToString(separator)
    }
    
    /**
     * Generate a content fingerprint combining multiple hashes.
     * Used for comprehensive document identification.
     */
    fun generateFingerprint(content: String): ContentFingerprint {
        return ContentFingerprint(
            sha512 = sha512(content),
            sha256 = sha256(content),
            contentLength = content.length,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Represents a comprehensive content fingerprint.
     */
    data class ContentFingerprint(
        val sha512: String,
        val sha256: String,
        val contentLength: Int,
        val timestamp: Long
    ) {
        /**
         * Get a compact representation of the fingerprint.
         */
        fun toCompactString(): String {
            return "SHA512:${sha512.take(16)}|SHA256:${sha256.take(16)}|LEN:$contentLength"
        }
    }
    
    /**
     * Check if a string is a valid SHA-512 hash format.
     */
    fun isValidSha512Format(hash: String): Boolean {
        return hash.length == 128 && hash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }
    
    /**
     * Check if a string is a valid SHA-256 hash format.
     */
    fun isValidSha256Format(hash: String): Boolean {
        return hash.length == 64 && hash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }
}
