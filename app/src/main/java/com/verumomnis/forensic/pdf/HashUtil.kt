package com.verumomnis.forensic.pdf

import java.security.MessageDigest

/**
 * SHA-512 hashing utility for forensic sealing.
 */
object HashUtil {
    
    /**
     * Compute SHA-512 hash of byte array.
     */
    fun sha512(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-512 hash of text.
     */
    fun sha512(text: String): String {
        return sha512(text.toByteArray(Charsets.UTF_8))
    }
}
