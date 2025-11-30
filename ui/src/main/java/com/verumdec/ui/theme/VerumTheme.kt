package com.verumdec.ui.theme

import android.graphics.Color

/**
 * VerumTheme - Application theme configuration.
 */
class VerumTheme {

    // Primary colors
    val primaryColor = Color.parseColor("#1A237E")
    val primaryDark = Color.parseColor("#0D1642")
    val primaryLight = Color.parseColor("#3949AB")

    // Accent colors
    val accentColor = Color.parseColor("#00BCD4")
    val accentLight = Color.parseColor("#4DD0E1")

    // Status colors
    val successColor = Color.parseColor("#4CAF50")
    val warningColor = Color.parseColor("#FF9800")
    val errorColor = Color.parseColor("#F44336")
    val infoColor = Color.parseColor("#2196F3")

    // Severity colors
    val criticalColor = Color.parseColor("#D32F2F")
    val highColor = Color.parseColor("#E64A19")
    val mediumColor = Color.parseColor("#FFA000")
    val lowColor = Color.parseColor("#689F38")

    // Background colors
    val backgroundColor = Color.parseColor("#FAFAFA")
    val surfaceColor = Color.WHITE
    val cardColor = Color.WHITE

    // Text colors
    val textPrimary = Color.parseColor("#212121")
    val textSecondary = Color.parseColor("#757575")
    val textHint = Color.parseColor("#BDBDBD")
    val textOnPrimary = Color.WHITE

    // Get severity color
    fun getSeverityColor(severity: String): Int {
        return when (severity.uppercase()) {
            "CRITICAL" -> criticalColor
            "HIGH" -> highColor
            "MEDIUM" -> mediumColor
            "LOW" -> lowColor
            else -> textSecondary
        }
    }

    // Get status color
    fun getStatusColor(status: String): Int {
        return when (status.uppercase()) {
            "SUCCESS", "COMPLETE" -> successColor
            "WARNING", "PENDING" -> warningColor
            "ERROR", "FAILED" -> errorColor
            "INFO", "PROCESSING" -> infoColor
            else -> textSecondary
        }
    }
}
