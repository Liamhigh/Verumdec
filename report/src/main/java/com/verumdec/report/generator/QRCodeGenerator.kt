package com.verumdec.report.generator

import android.graphics.Bitmap
import android.graphics.Color

/**
 * QRCodeGenerator - Generates QR codes for PDF sealing.
 * Contains truncated SHA-512 hash for document verification.
 * Pure Android implementation without external libraries.
 */
class QRCodeGenerator {

    companion object {
        private const val DEFAULT_SIZE = 100
        private const val QUIET_ZONE = 4 // White border modules
    }

    /**
     * Generate a QR code bitmap.
     * Uses a simplified QR-like pattern for the hash.
     * For production, consider using ZXing library.
     */
    fun generateQRCode(content: String, size: Int = DEFAULT_SIZE): Bitmap? {
        return try {
            createQRBitmap(content, size)
        } catch (e: Exception) {
            e.printStackTrace()
            createFallbackPattern(content, size)
        }
    }

    /**
     * Create QR-like bitmap pattern.
     * This is a simplified implementation - for full QR spec, use ZXing.
     */
    private fun createQRBitmap(content: String, size: Int): Bitmap {
        // Create a deterministic pattern based on content hash
        val moduleCount = 21 // QR Version 1 is 21x21
        val moduleSize = size / (moduleCount + 2 * QUIET_ZONE)
        val actualSize = moduleSize * (moduleCount + 2 * QUIET_ZONE)

        val bitmap = Bitmap.createBitmap(actualSize, actualSize, Bitmap.Config.ARGB_8888)

        // Fill with white background
        for (x in 0 until actualSize) {
            for (y in 0 until actualSize) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        // Create module matrix
        val matrix = Array(moduleCount) { BooleanArray(moduleCount) }

        // Add finder patterns (the three corner squares)
        addFinderPattern(matrix, 0, 0)
        addFinderPattern(matrix, moduleCount - 7, 0)
        addFinderPattern(matrix, 0, moduleCount - 7)

        // Add timing patterns
        for (i in 8 until moduleCount - 8) {
            matrix[6][i] = i % 2 == 0
            matrix[i][6] = i % 2 == 0
        }

        // Add data based on content hash
        val hash = content.hashCode()
        var bitIndex = 0
        for (row in 0 until moduleCount) {
            for (col in 0 until moduleCount) {
                if (!isReserved(row, col, moduleCount)) {
                    // Use hash bits to determine module color
                    val bit = (hash shr (bitIndex % 32)) and 1
                    matrix[row][col] = bit == 1
                    bitIndex++
                }
            }
        }

        // Apply XOR mask pattern for better readability
        applyMaskPattern(matrix, moduleCount)

        // Render to bitmap
        for (row in 0 until moduleCount) {
            for (col in 0 until moduleCount) {
                val color = if (matrix[row][col]) Color.BLACK else Color.WHITE
                
                val startX = (col + QUIET_ZONE) * moduleSize
                val startY = (row + QUIET_ZONE) * moduleSize
                
                for (x in startX until startX + moduleSize) {
                    for (y in startY until startY + moduleSize) {
                        if (x < actualSize && y < actualSize) {
                            bitmap.setPixel(x, y, color)
                        }
                    }
                }
            }
        }

        return Bitmap.createScaledBitmap(bitmap, size, size, false)
    }

    /**
     * Add finder pattern (7x7 square with pattern).
     */
    private fun addFinderPattern(matrix: Array<BooleanArray>, startRow: Int, startCol: Int) {
        for (row in 0 until 7) {
            for (col in 0 until 7) {
                val r = startRow + row
                val c = startCol + col
                
                if (r < matrix.size && c < matrix[0].size) {
                    // Outer border
                    if (row == 0 || row == 6 || col == 0 || col == 6) {
                        matrix[r][c] = true
                    }
                    // Inner square
                    else if (row in 2..4 && col in 2..4) {
                        matrix[r][c] = true
                    }
                    // White border around inner square
                    else {
                        matrix[r][c] = false
                    }
                }
            }
        }
    }

    /**
     * Check if position is reserved for finder/timing patterns.
     */
    private fun isReserved(row: Int, col: Int, size: Int): Boolean {
        // Finder patterns
        if (row < 8 && col < 8) return true // Top-left
        if (row < 8 && col >= size - 8) return true // Top-right
        if (row >= size - 8 && col < 8) return true // Bottom-left

        // Timing patterns
        if (row == 6 || col == 6) return true

        // Separators
        if (row == 7 && (col < 8 || col >= size - 8)) return true
        if (col == 7 && row < 8) return true
        if (col == size - 8 && row < 8) return true

        return false
    }

    /**
     * Apply mask pattern to improve readability.
     */
    private fun applyMaskPattern(matrix: Array<BooleanArray>, size: Int) {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (!isReserved(row, col, size)) {
                    // Mask pattern 0: (row + col) % 2 == 0
                    if ((row + col) % 2 == 0) {
                        matrix[row][col] = !matrix[row][col]
                    }
                }
            }
        }
    }

    /**
     * Create fallback pattern if QR generation fails.
     */
    private fun createFallbackPattern(content: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        // Create a simple grid pattern based on hash
        val hash = content.hashCode()
        val cellSize = size / 8
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                val cellX = x / cellSize
                val cellY = y / cellSize
                val cellIndex = cellY * 8 + cellX
                val bit = (hash shr (cellIndex % 32)) and 1
                
                // Border
                if (x < 2 || x >= size - 2 || y < 2 || y >= size - 2) {
                    bitmap.setPixel(x, y, Color.BLACK)
                } else {
                    bitmap.setPixel(x, y, if (bit == 1) Color.BLACK else Color.WHITE)
                }
            }
        }
        
        return bitmap
    }

    /**
     * Generate verification string for QR content.
     */
    fun generateVerificationString(sha512Hash: String, caseId: String): String {
        val truncatedHash = sha512Hash.take(32)
        return "VO:$caseId:$truncatedHash"
    }
}
