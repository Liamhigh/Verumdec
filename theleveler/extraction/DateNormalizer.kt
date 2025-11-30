package ai.verum.theleveler.extraction

/**
 * Normalizes various date formats into a consistent sortable format.
 * Input: flexible string timestamps
 * Output: normalized ISO-like string for sorting
 */
object DateNormalizer {

    private val monthMap = mapOf(
        "january" to "01", "jan" to "01",
        "february" to "02", "feb" to "02",
        "march" to "03", "mar" to "03",
        "april" to "04", "apr" to "04",
        "may" to "05",
        "june" to "06", "jun" to "06",
        "july" to "07", "jul" to "07",
        "august" to "08", "aug" to "08",
        "september" to "09", "sep" to "09", "sept" to "09",
        "october" to "10", "oct" to "10",
        "november" to "11", "nov" to "11",
        "december" to "12", "dec" to "12"
    )

    fun normalize(timestamp: String?): String? {
        if (timestamp == null) return null
        
        val cleaned = timestamp.lowercase().trim()
        
        // Try YYYY-MM-DD format
        val isoPattern = Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})")
        isoPattern.find(cleaned)?.let { match ->
            val (year, month, day) = match.destructured
            return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
        }
        
        // Try DD/MM/YYYY or DD-MM-YYYY
        val dmy = Regex("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})")
        dmy.find(cleaned)?.let { match ->
            val (day, month, year) = match.destructured
            return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
        }
        
        // Try Month DD, YYYY
        val monthDayYear = Regex("([a-z]+)\\s+(\\d{1,2}),?\\s+(\\d{4})")
        monthDayYear.find(cleaned)?.let { match ->
            val monthName = match.groupValues[1]
            val day = match.groupValues[2]
            val year = match.groupValues[3]
            val month = monthMap[monthName] ?: return null
            return "$year-$month-${day.padStart(2, '0')}"
        }
        
        // Try DD Month YYYY
        val dayMonthYear = Regex("(\\d{1,2})\\s+([a-z]+)\\s+(\\d{4})")
        dayMonthYear.find(cleaned)?.let { match ->
            val day = match.groupValues[1]
            val monthName = match.groupValues[2]
            val year = match.groupValues[3]
            val month = monthMap[monthName] ?: return null
            return "$year-$month-${day.padStart(2, '0')}"
        }
        
        return null
    }

    fun compare(a: String?, b: String?): Int {
        val normA = normalize(a)
        val normB = normalize(b)
        
        return when {
            normA == null && normB == null -> 0
            normA == null -> 1
            normB == null -> -1
            else -> normA.compareTo(normB)
        }
    }
}
