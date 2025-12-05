package com.verumdec.forensic

import java.security.MessageDigest

class FileSealer {
    fun generateSeal(input: String): String {
        val bytes = input.toByteArray()
        return sha512(bytes)
    }

    private fun sha512(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
