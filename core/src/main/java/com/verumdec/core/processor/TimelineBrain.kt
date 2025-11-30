package com.verumdec.core.processor

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * TimelineBrain - Auto-build legal timeline from evidence timestamps
 *
 * Analyzes evidence documents to extract dates and timestamps,
 * then constructs a chronological timeline of events for legal analysis.
 *
 * Operates fully offline without external dependencies.
 */
class TimelineBrain {

    companion object {
        // Date patterns to detect
        private val DATE_PATTERNS = listOf(
            "dd/MM/yyyy HH:mm:ss" to Regex("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}"),
            "dd/MM/yyyy HH:mm" to Regex("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}"),
            "dd/MM/yyyy" to Regex("\\d{2}/\\d{2}/\\d{4}"),
            "MM/dd/yyyy HH:mm:ss" to Regex("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}"),
            "MM/dd/yyyy HH:mm" to Regex("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}"),
            "MM/dd/yyyy" to Regex("\\d{2}/\\d{2}/\\d{4}"),
            "yyyy-MM-dd HH:mm:ss" to Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}"),
            "yyyy-MM-dd'T'HH:mm:ss" to Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"),
            "yyyy-MM-dd" to Regex("\\d{4}-\\d{2}-\\d{2}"),
            "dd MMM yyyy HH:mm" to Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}\\s+\\d{2}:\\d{2}", RegexOption.IGNORE_CASE),
            "dd MMM yyyy" to Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}", RegexOption.IGNORE_CASE),
            "MMM dd, yyyy" to Regex("(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}", RegexOption.IGNORE_CASE),
            "MMMM dd, yyyy" to Regex("(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4}", RegexOption.IGNORE_CASE)
        )

        // Event classification keywords
        private val PAYMENT_KEYWORDS = listOf("paid", "payment", "transfer", "sent money", "received", "deposited", "withdrawn", "invoice")
        private val PROMISE_KEYWORDS = listOf("will", "shall", "promise", "commit", "agree to", "by monday", "by friday", "next week", "tomorrow")
        private val DENIAL_KEYWORDS = listOf("never", "didn't", "did not", "wasn't", "was not", "no deal", "not true", "deny", "reject")
        private val ADMISSION_KEYWORDS = listOf("admit", "confess", "actually", "truth is", "honestly", "yes i", "i did")
        private val AGREEMENT_KEYWORDS = listOf("agreed", "contract", "signed", "accepted", "confirmed", "deal")
        private val BREACH_KEYWORDS = listOf("breach", "violated", "failed to", "did not comply", "broke", "default")
    }

    /**
     * Build timeline from evidence documents.
     *
     * @param documents List of document inputs with content and metadata
     * @return TimelineBrainResult containing timeline or error
     */
    fun buildTimeline(documents: List<EvidenceDocument>): TimelineBrainResult {
        return try {
            if (documents.isEmpty()) {
                return TimelineBrainResult.Failure(
                    error = "No documents provided for timeline generation",
                    errorCode = TimelineErrorCode.INSUFFICIENT_DATA
                )
            }

            val events = mutableListOf<TimelineEventResult>()

            // Extract events from each document
            for (document in documents) {
                events.addAll(extractEvents(document))
            }

            if (events.isEmpty()) {
                return TimelineBrainResult.Failure(
                    error = "No dated events found in the provided documents",
                    errorCode = TimelineErrorCode.NO_DATES_FOUND
                )
            }

            // Sort events chronologically
            val sortedEvents = events.sortedBy { it.date }

            // Calculate date range
            val dateRange = DateRange(
                start = sortedEvents.first().date,
                end = sortedEvents.last().date,
                durationDays = ((sortedEvents.last().date.time - sortedEvents.first().date.time) / (24 * 60 * 60 * 1000)).toInt()
            )

            // Calculate entity participation
            val entityParticipation = calculateEntityParticipation(sortedEvents)

            // Identify key moments
            val keyMoments = identifyKeyMoments(sortedEvents)

            // Detect timeline gaps
            val gaps = detectGaps(sortedEvents)

            // Generate warnings
            val warnings = generateWarnings(sortedEvents, gaps)

            TimelineBrainResult.Success(
                events = sortedEvents,
                dateRange = dateRange,
                entityParticipation = entityParticipation,
                keyMoments = keyMoments,
                gaps = gaps,
                warnings = warnings
            )
        } catch (e: Exception) {
            TimelineBrainResult.Failure(
                error = "Timeline generation error: ${e.message}",
                errorCode = TimelineErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Evidence document input.
     */
    data class EvidenceDocument(
        val id: String,
        val name: String,
        val content: String,
        val creationDate: Date? = null,
        val modificationDate: Date? = null,
        val entityIds: List<String> = emptyList(),
        val type: String = "document"
    )

    /**
     * Extract events from a document.
     */
    private fun extractEvents(document: EvidenceDocument): List<TimelineEventResult> {
        val events = mutableListOf<TimelineEventResult>()

        // Add document creation/modification as events
        document.creationDate?.let { date ->
            events.add(TimelineEventResult(
                date = date,
                description = "Document created: ${document.name}",
                sourceDocumentId = document.id,
                sourceDocumentName = document.name,
                entityIds = document.entityIds,
                eventType = TimelineEventType.DOCUMENT_CREATED,
                significance = AnomalySeverity.LOW,
                verified = true
            ))
        }

        document.modificationDate?.let { date ->
            if (date != document.creationDate) {
                events.add(TimelineEventResult(
                    date = date,
                    description = "Document modified: ${document.name}",
                    sourceDocumentId = document.id,
                    sourceDocumentName = document.name,
                    entityIds = document.entityIds,
                    eventType = TimelineEventType.DOCUMENT_MODIFIED,
                    significance = AnomalySeverity.LOW,
                    verified = true
                ))
            }
        }

        // Extract dated events from content
        events.addAll(extractDatedEvents(document))

        return events
    }

    /**
     * Extract events with dates from document content.
     */
    private fun extractDatedEvents(document: EvidenceDocument): List<TimelineEventResult> {
        val events = mutableListOf<TimelineEventResult>()
        val sentences = document.content.split(Regex("[.!?\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (sentence in sentences) {
            // Try to find a date in the sentence
            val dateResult = extractDate(sentence)
            if (dateResult != null) {
                val (date, matchedText) = dateResult
                val eventType = classifyEvent(sentence)
                val significance = determineSignificance(eventType, sentence)
                val entityIds = extractEntities(sentence, document.entityIds)

                events.add(TimelineEventResult(
                    date = date,
                    description = sentence.take(500),
                    sourceDocumentId = document.id,
                    sourceDocumentName = document.name,
                    entityIds = entityIds,
                    eventType = eventType,
                    significance = significance,
                    verified = false
                ))
            }
        }

        return events
    }

    /**
     * Extract date from text.
     */
    private fun extractDate(text: String): Pair<Date, String>? {
        for ((format, pattern) in DATE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val dateStr = match.value
                val date = parseDate(dateStr, format)
                if (date != null) {
                    return date to dateStr
                }
            }
        }
        return null
    }

    /**
     * Parse date with specific format.
     */
    private fun parseDate(dateStr: String, format: String): Date? {
        return try {
            val adjustedFormat = format.replace("'T'", "'T'")
            SimpleDateFormat(adjustedFormat, Locale.US).parse(dateStr.trim())
        } catch (_: Exception) {
            // Try alternative formats
            val alternativeFormats = listOf(
                "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd",
                "d MMM yyyy", "MMM d, yyyy", "MMMM d, yyyy"
            )
            for (altFormat in alternativeFormats) {
                try {
                    return SimpleDateFormat(altFormat, Locale.US).parse(dateStr.trim())
                } catch (_: Exception) {
                    continue
                }
            }
            null
        }
    }

    /**
     * Classify event type based on content.
     */
    private fun classifyEvent(text: String): TimelineEventType {
        val lowerText = text.lowercase()

        return when {
            PAYMENT_KEYWORDS.any { lowerText.contains(it) } -> TimelineEventType.PAYMENT
            PROMISE_KEYWORDS.any { lowerText.contains(it) } -> TimelineEventType.PROMISE
            DENIAL_KEYWORDS.any { lowerText.contains(it) } -> TimelineEventType.DENIAL
            ADMISSION_KEYWORDS.any { lowerText.contains(it) } -> TimelineEventType.ADMISSION
            AGREEMENT_KEYWORDS.any { lowerText.contains(it) } -> TimelineEventType.AGREEMENT
            BREACH_KEYWORDS.any { lowerText.contains(it) } -> TimelineEventType.BREACH
            lowerText.contains("email") || lowerText.contains("message") || lowerText.contains("call") -> TimelineEventType.COMMUNICATION
            else -> TimelineEventType.OTHER
        }
    }

    /**
     * Determine event significance.
     */
    private fun determineSignificance(eventType: TimelineEventType, text: String): AnomalySeverity {
        val lowerText = text.lowercase()

        // Check for high-impact keywords
        val criticalKeywords = listOf("fraud", "theft", "criminal", "illegal", "perjury", "forged", "falsified")
        val highKeywords = listOf("breach", "default", "terminate", "lawsuit", "court", "legal action")

        return when {
            criticalKeywords.any { lowerText.contains(it) } -> AnomalySeverity.CRITICAL
            highKeywords.any { lowerText.contains(it) } -> AnomalySeverity.HIGH
            eventType in listOf(TimelineEventType.ADMISSION, TimelineEventType.CONTRADICTION, TimelineEventType.BREACH) -> AnomalySeverity.HIGH
            eventType in listOf(TimelineEventType.PAYMENT, TimelineEventType.AGREEMENT) -> AnomalySeverity.MEDIUM
            eventType in listOf(TimelineEventType.PROMISE, TimelineEventType.DENIAL) -> AnomalySeverity.MEDIUM
            else -> AnomalySeverity.LOW
        }
    }

    /**
     * Extract entity IDs mentioned in text.
     */
    private fun extractEntities(text: String, documentEntityIds: List<String>): List<String> {
        // For simplicity, return document entity IDs
        // Real implementation would use NER or entity matching
        return documentEntityIds
    }

    /**
     * Calculate entity participation in timeline.
     */
    private fun calculateEntityParticipation(events: List<TimelineEventResult>): Map<String, Int> {
        val participation = mutableMapOf<String, Int>()

        for (event in events) {
            for (entityId in event.entityIds) {
                participation[entityId] = participation.getOrDefault(entityId, 0) + 1
            }
        }

        return participation
    }

    /**
     * Identify key moments in the timeline.
     */
    private fun identifyKeyMoments(events: List<TimelineEventResult>): List<KeyMoment> {
        val keyMoments = mutableListOf<KeyMoment>()

        // Find first event
        if (events.isNotEmpty()) {
            val first = events.first()
            keyMoments.add(KeyMoment(
                date = first.date,
                description = "Timeline begins: ${first.description.take(100)}",
                significance = AnomalySeverity.MEDIUM,
                relatedEventIds = listOf(first.id)
            ))
        }

        // Find critical and high significance events
        for (event in events) {
            if (event.significance in listOf(AnomalySeverity.CRITICAL, AnomalySeverity.HIGH)) {
                keyMoments.add(KeyMoment(
                    date = event.date,
                    description = "Key event: ${event.description.take(100)}",
                    significance = event.significance,
                    relatedEventIds = listOf(event.id)
                ))
            }
        }

        // Find last event
        if (events.size > 1) {
            val last = events.last()
            keyMoments.add(KeyMoment(
                date = last.date,
                description = "Timeline ends: ${last.description.take(100)}",
                significance = AnomalySeverity.MEDIUM,
                relatedEventIds = listOf(last.id)
            ))
        }

        // Find turning points (event type changes)
        var lastType: TimelineEventType? = null
        for (event in events) {
            if (lastType != null && event.eventType != lastType) {
                val typeChange = "${lastType.name} -> ${event.eventType.name}"
                if (isSignificantTypeChange(lastType, event.eventType)) {
                    keyMoments.add(KeyMoment(
                        date = event.date,
                        description = "Narrative shift: $typeChange",
                        significance = AnomalySeverity.MEDIUM,
                        relatedEventIds = listOf(event.id)
                    ))
                }
            }
            lastType = event.eventType
        }

        return keyMoments.distinctBy { it.date.time }.sortedBy { it.date }
    }

    /**
     * Check if event type change is significant.
     */
    private fun isSignificantTypeChange(from: TimelineEventType, to: TimelineEventType): Boolean {
        val significantChanges = listOf(
            TimelineEventType.AGREEMENT to TimelineEventType.BREACH,
            TimelineEventType.PROMISE to TimelineEventType.DENIAL,
            TimelineEventType.DENIAL to TimelineEventType.ADMISSION,
            TimelineEventType.COMMUNICATION to TimelineEventType.CONTRADICTION
        )
        return (from to to) in significantChanges
    }

    /**
     * Detect gaps in the timeline.
     */
    private fun detectGaps(events: List<TimelineEventResult>): List<TimelineGap> {
        val gaps = mutableListOf<TimelineGap>()

        if (events.size < 2) return gaps

        for (i in 0 until events.size - 1) {
            val current = events[i]
            val next = events[i + 1]

            val gapMs = next.date.time - current.date.time
            val gapDays = (gapMs / (24 * 60 * 60 * 1000)).toInt()

            // Flag gaps longer than 7 days
            if (gapDays > 7) {
                val suspiciousLevel = when {
                    gapDays > 90 -> AnomalySeverity.HIGH
                    gapDays > 30 -> AnomalySeverity.MEDIUM
                    else -> AnomalySeverity.LOW
                }

                gaps.add(TimelineGap(
                    startDate = current.date,
                    endDate = next.date,
                    durationDays = gapDays,
                    description = "No documented activity for $gapDays days",
                    suspiciousLevel = suspiciousLevel
                ))
            }
        }

        return gaps
    }

    /**
     * Generate warnings based on timeline analysis.
     */
    private fun generateWarnings(events: List<TimelineEventResult>, gaps: List<TimelineGap>): List<String> {
        val warnings = mutableListOf<String>()

        // Check for suspicious gaps
        val suspiciousGaps = gaps.filter { it.suspiciousLevel in listOf(AnomalySeverity.HIGH, AnomalySeverity.MEDIUM) }
        if (suspiciousGaps.isNotEmpty()) {
            warnings.add("${suspiciousGaps.size} significant timeline gap(s) detected")
        }

        // Check for contradictions
        val contradictionEvents = events.filter { it.eventType == TimelineEventType.CONTRADICTION }
        if (contradictionEvents.isNotEmpty()) {
            warnings.add("${contradictionEvents.size} contradiction event(s) identified")
        }

        // Check for admissions
        val admissionEvents = events.filter { it.eventType == TimelineEventType.ADMISSION }
        if (admissionEvents.isNotEmpty()) {
            warnings.add("${admissionEvents.size} admission(s) detected in timeline")
        }

        // Check for breach after agreement
        val agreementIndex = events.indexOfFirst { it.eventType == TimelineEventType.AGREEMENT }
        val breachIndex = events.indexOfFirst { it.eventType == TimelineEventType.BREACH }
        if (agreementIndex >= 0 && breachIndex > agreementIndex) {
            warnings.add("Breach detected after agreement - review contract terms")
        }

        // Check for critical events
        val criticalEvents = events.filter { it.significance == AnomalySeverity.CRITICAL }
        if (criticalEvents.isNotEmpty()) {
            warnings.add("${criticalEvents.size} critical event(s) require immediate review")
        }

        return warnings
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: TimelineBrainResult): String {
        return when (result) {
            is TimelineBrainResult.Success -> buildSuccessJson(result)
            is TimelineBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: TimelineBrainResult.Success): String {
        val eventsJson = result.events.joinToString(",") { e ->
            val entityIdsJson = e.entityIds.joinToString(",") { "\"$it\"" }
            """{"id":"${e.id}","date":${e.date.time},"description":"${escapeJson(e.description.take(200))}","sourceDocumentId":"${e.sourceDocumentId}","sourceDocumentName":"${escapeJson(e.sourceDocumentName)}","entityIds":[$entityIdsJson],"eventType":"${e.eventType}","significance":"${e.significance}","verified":${e.verified}}"""
        }
        val participationJson = result.entityParticipation.entries.joinToString(",") {
            "\"${it.key}\":${it.value}"
        }
        val keyMomentsJson = result.keyMoments.joinToString(",") { km ->
            val relatedIdsJson = km.relatedEventIds.joinToString(",") { "\"$it\"" }
            """{"date":${km.date.time},"description":"${escapeJson(km.description)}","significance":"${km.significance}","relatedEventIds":[$relatedIdsJson]}"""
        }
        val gapsJson = result.gaps.joinToString(",") { g ->
            """{"startDate":${g.startDate.time},"endDate":${g.endDate.time},"durationDays":${g.durationDays},"description":"${escapeJson(g.description)}","suspiciousLevel":"${g.suspiciousLevel}"}"""
        }
        val warningsJson = result.warnings.joinToString(",") { "\"${escapeJson(it)}\"" }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "timelineId": "${result.timelineId}",
            "eventCount": ${result.events.size},
            "events": [$eventsJson],
            "dateRange": {
                "start": ${result.dateRange.start.time},
                "end": ${result.dateRange.end.time},
                "durationDays": ${result.dateRange.durationDays}
            },
            "entityParticipation": {$participationJson},
            "keyMoments": [$keyMomentsJson],
            "gaps": [$gapsJson],
            "warnings": [$warningsJson]
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: TimelineBrainResult.Failure): String {
        return """
        {
            "success": false,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "error": "${escapeJson(result.error)}",
            "errorCode": "${result.errorCode}"
        }
        """.trimIndent()
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
