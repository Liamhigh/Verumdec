package com.verumomnis.forensic.model

import java.io.File

/**
 * Data class representing the result of a forensic analysis.
 */
data class ForensicResult(
    val contradictions: List<String>,
    val behaviouralFlags: List<String>,
    val pdfFile: File
)
