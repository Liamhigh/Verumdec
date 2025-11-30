package com.verumdec.core.util

import java.io.*

/**
 * Utility class for stream operations.
 */
object StreamUtils {

    /**
     * Read entire stream to string.
     */
    fun readToString(inputStream: InputStream, charset: String = "UTF-8"): String {
        return inputStream.bufferedReader(charset(charset)).use { it.readText() }
    }

    /**
     * Read entire stream to byte array.
     */
    fun readToBytes(inputStream: InputStream): ByteArray {
        return inputStream.readBytes()
    }

    /**
     * Copy stream to file.
     */
    fun copyToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
    }

    /**
     * Copy stream to output stream.
     */
    fun copy(input: InputStream, output: OutputStream, bufferSize: Int = 8192): Long {
        return input.copyTo(output, bufferSize)
    }

    /**
     * Read first N bytes from stream.
     */
    fun readFirstBytes(inputStream: InputStream, count: Int): ByteArray {
        val buffer = ByteArray(count)
        val bytesRead = inputStream.read(buffer, 0, count)
        return if (bytesRead < count) buffer.copyOf(bytesRead) else buffer
    }

    /**
     * Skip bytes in stream.
     */
    fun skip(inputStream: InputStream, count: Long): Long {
        return inputStream.skip(count)
    }

    /**
     * Create buffered input stream.
     */
    fun buffered(inputStream: InputStream, bufferSize: Int = 8192): BufferedInputStream {
        return BufferedInputStream(inputStream, bufferSize)
    }

    /**
     * Create buffered output stream.
     */
    fun buffered(outputStream: OutputStream, bufferSize: Int = 8192): BufferedOutputStream {
        return BufferedOutputStream(outputStream, bufferSize)
    }

    /**
     * Close stream safely.
     */
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (_: Exception) {
            // Ignore
        }
    }

    /**
     * Read lines from stream.
     */
    fun readLines(inputStream: InputStream, charset: String = "UTF-8"): List<String> {
        return inputStream.bufferedReader(charset(charset)).readLines()
    }

    /**
     * Write string to output stream.
     */
    fun writeString(outputStream: OutputStream, content: String, charset: String = "UTF-8") {
        outputStream.write(content.toByteArray(charset(charset)))
    }

    /**
     * Create ByteArrayInputStream from string.
     */
    fun fromString(content: String, charset: String = "UTF-8"): ByteArrayInputStream {
        return ByteArrayInputStream(content.toByteArray(charset(charset)))
    }

    /**
     * Detect file signature (magic bytes).
     */
    fun detectFileSignature(inputStream: InputStream): FileSignature {
        val header = readFirstBytes(inputStream, 16)
        
        return when {
            // PDF: %PDF
            header.size >= 4 && header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && 
                header[2] == 0x44.toByte() && header[3] == 0x46.toByte() -> FileSignature.PDF
            
            // PNG: 89 50 4E 47
            header.size >= 4 && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && 
                header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() -> FileSignature.PNG
            
            // JPEG: FF D8 FF
            header.size >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && 
                header[2] == 0xFF.toByte() -> FileSignature.JPEG
            
            // GIF: GIF8
            header.size >= 4 && header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && 
                header[2] == 0x46.toByte() && header[3] == 0x38.toByte() -> FileSignature.GIF
            
            // MP4/MOV: ftyp at offset 4
            header.size >= 8 && header[4] == 0x66.toByte() && header[5] == 0x74.toByte() && 
                header[6] == 0x79.toByte() && header[7] == 0x70.toByte() -> FileSignature.MP4
            
            // MP3: ID3 or FF FB
            header.size >= 3 && (header[0] == 0x49.toByte() && header[1] == 0x44.toByte() && 
                header[2] == 0x33.toByte()) || (header[0] == 0xFF.toByte() && 
                (header[1].toInt() and 0xE0) == 0xE0) -> FileSignature.MP3
            
            // ZIP: PK
            header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() -> FileSignature.ZIP
            
            else -> FileSignature.UNKNOWN
        }
    }

    /**
     * File signature types.
     */
    enum class FileSignature {
        PDF, PNG, JPEG, GIF, MP4, MP3, ZIP, UNKNOWN
    }
}
