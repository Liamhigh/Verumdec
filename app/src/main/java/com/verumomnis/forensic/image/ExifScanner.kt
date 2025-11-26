package com.verumomnis.forensic.image

/**
 * EXIF metadata scanner for detecting tampering.
 */
object ExifScanner {

    /**
     * Scan EXIF metadata for anomalies.
     */
    fun scan(exif: Map<String, String>): List<String> {
        val anomalies = mutableListOf<String>()

        // Missing timestamp
        if (!exif.containsKey("DateTimeOriginal")) {
            anomalies.add("Missing DateTimeOriginal timestamp")
        }

        // Camera manufacturer erased
        if (exif["Make"] == "Unknown" || exif["Make"].isNullOrEmpty()) {
            anomalies.add("Camera manufacturer metadata missing/erased")
        }

        // Editing software detected
        val software = exif["Software"] ?: ""
        if (software.contains("Photoshop", true)) {
            anomalies.add("Edited by software: Adobe Photoshop detected")
        }
        if (software.contains("GIMP", true)) {
            anomalies.add("Edited by software: GIMP detected")
        }
        if (software.contains("Lightroom", true)) {
            anomalies.add("Edited by software: Adobe Lightroom detected")
        }

        // Partial GPS metadata (tampering indicator)
        val hasLat = exif.containsKey("GPSLatitude")
        val hasLong = exif.containsKey("GPSLongitude")
        if (hasLat != hasLong) {
            anomalies.add("Partial GPS metadata (tampering likely)")
        }

        // Mismatched timestamps
        val dateOriginal = exif["DateTimeOriginal"]
        val dateModified = exif["DateTime"]
        if (dateOriginal != null && dateModified != null && dateOriginal != dateModified) {
            anomalies.add("Timestamp mismatch: original vs modified dates differ")
        }

        return anomalies
    }
}
