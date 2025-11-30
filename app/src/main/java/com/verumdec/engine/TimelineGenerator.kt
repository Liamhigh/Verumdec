package com.verumdec.engine

import com.verumdec.data.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Timeline Generator
 * Builds chronological timelines from evidence.
 * Implements clustering, gap analysis, and pattern detection.
 */
class TimelineGenerator {

    // Date patterns to detect
    private val datePatterns = listOf(
        "dd/MM/yyyy HH:mm:ss" to Regex("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}"),
        "dd/MM/yyyy HH:mm" to Regex("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}"),
        "dd/MM/yyyy" to Regex("\\d{2}/\\d{2}/\\d{4}"),
        "yyyy-MM-dd HH:mm:ss" to Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}"),
        "yyyy-MM-dd" to Regex("\\d{4}-\\d{2}-\\d{2}"),
        "dd MMM yyyy" to Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}", RegexOption.IGNORE_CASE),
        "MMM dd, yyyy" to Regex("(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}", RegexOption.IGNORE_CASE)
    )

    // WhatsApp message pattern
    private val whatsappPattern = Regex("\\[(\\d{1,2}/\\d{1,2}/\\d{2,4},\\s*\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?)\\]\\s*([^:]+):\\s*(.+)", RegexOption.IGNORE_CASE)
    
    // Common event keywords
    private val paymentKeywords = listOf("paid", "payment", "transfer", "sent money", "received", "r\\d+", "\\$\\d+", "€\\d+", "£\\d+")
    private val promiseKeywords = listOf("will", "shall", "promise", "going to", "commit", "by monday", "by friday", "tomorrow", "next week")
    private val denialKeywords = listOf("never", "didn't", "did not", "wasn't", "was not", "no deal", "not true", "false", "lie")
    private val admissionKeywords = listOf("admit", "confess", "actually", "truth is", "honestly", "i did", "yes i")

    /**
     * Generate timeline from all evidence.
     */
    fun generateTimeline(evidenceList: List<Evidence>, entities: List<Entity>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        
        for (evidence in evidenceList) {
            if (evidence.extractedText.isEmpty()) continue
            
            when (evidence.type) {
                EvidenceType.WHATSAPP -> events.addAll(parseWhatsAppMessages(evidence, entities))
                EvidenceType.EMAIL -> events.addAll(parseEmailEvents(evidence, entities))
                else -> events.addAll(parseGenericEvents(evidence, entities))
            }
        }
        
        return events.sortedBy { it.date }
    }

    /**
     * Cluster events by temporal proximity.
     * Groups events that occur within the same time window.
     */
    fun clusterEvents(events: List<TimelineEvent>, windowHours: Int = 24): List<EventCluster> {
        if (events.isEmpty()) return emptyList()
        
        val clusters = mutableListOf<EventCluster>()
        val sorted = events.sortedBy { it.date }
        var currentCluster = mutableListOf<TimelineEvent>()
        val windowMs = TimeUnit.HOURS.toMillis(windowHours.toLong())
        
        for (event in sorted) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(event)
            } else {
                val lastEvent = currentCluster.last()
                val timeDiff = event.date.time - lastEvent.date.time
                
                if (timeDiff <= windowMs) {
                    currentCluster.add(event)
                } else {
                    clusters.add(createCluster(currentCluster))
                    currentCluster = mutableListOf(event)
                }
            }
        }
        
        if (currentCluster.isNotEmpty()) {
            clusters.add(createCluster(currentCluster))
        }
        
        return clusters
    }

    /**
     * Analyze gaps in the timeline.
     * Identifies unusual periods of silence or missing activity.
     */
    fun analyzeGaps(events: List<TimelineEvent>): List<TimelineGap> {
        if (events.size < 2) return emptyList()
        
        val gaps = mutableListOf<TimelineGap>()
        val sorted = events.sortedBy { it.date }
        
        // Calculate average gap to determine what's unusual
        val allGapMs = mutableListOf<Long>()
        for (i in 1 until sorted.size) {
            allGapMs.add(sorted[i].date.time - sorted[i - 1].date.time)
        }
        
        val avgGapMs = if (allGapMs.isNotEmpty()) allGapMs.average() else 0.0
        val unusualThreshold = avgGapMs * 3 // 3x average is unusual
        
        for (i in 1 until sorted.size) {
            val gapMs = sorted[i].date.time - sorted[i - 1].date.time
            val gapDays = TimeUnit.MILLISECONDS.toDays(gapMs).toInt()
            
            if (gapDays >= 1) {
                val isUnusual = gapMs > unusualThreshold
                val precedingEvent = sorted[i - 1]
                val followingEvent = sorted[i]
                
                gaps.add(TimelineGap(
                    startDate = precedingEvent.date,
                    endDate = followingEvent.date,
                    durationDays = gapDays,
                    isUnusual = isUnusual,
                    precedingEventType = precedingEvent.eventType,
                    followingEventType = followingEvent.eventType,
                    involvedEntityIds = (precedingEvent.entityIds + followingEvent.entityIds).distinct()
                ))
            }
        }
        
        return gaps.filter { it.isUnusual || it.durationDays >= 7 }
    }

    /**
     * Detect patterns in timeline (weekly, monthly, etc.)
     */
    fun detectPatterns(events: List<TimelineEvent>): List<TimelinePattern> {
        val patterns = mutableListOf<TimelinePattern>()
        
        // Group by day of week
        val byDayOfWeek = events.groupBy { event ->
            val calendar = Calendar.getInstance()
            calendar.time = event.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }
        
        // Find recurring patterns
        for ((day, dayEvents) in byDayOfWeek) {
            if (dayEvents.size >= 3) {
                val dayName = getDayName(day)
                patterns.add(TimelinePattern(
                    patternType = PatternType.WEEKLY,
                    description = "Activity peak on $dayName",
                    frequency = dayEvents.size,
                    confidence = calculatePatternConfidence(dayEvents.size, events.size)
                ))
            }
        }
        
        // Check for monthly patterns
        val byDayOfMonth = events.groupBy { event ->
            val calendar = Calendar.getInstance()
            calendar.time = event.date
            calendar.get(Calendar.DAY_OF_MONTH)
        }
        
        for ((day, dayEvents) in byDayOfMonth) {
            if (dayEvents.size >= 3 && day <= 28) {
                patterns.add(TimelinePattern(
                    patternType = PatternType.MONTHLY,
                    description = "Recurring activity on day $day of month",
                    frequency = dayEvents.size,
                    confidence = calculatePatternConfidence(dayEvents.size, events.size)
                ))
            }
        }
        
        return patterns.sortedByDescending { it.confidence }
    }

    /**
     * Get timeline summary statistics.
     */
    fun getTimelineSummary(events: List<TimelineEvent>): TimelineSummary {
        if (events.isEmpty()) {
            return TimelineSummary(
                totalEvents = 0,
                startDate = null,
                endDate = null,
                durationDays = 0,
                eventsByType = emptyMap(),
                averageEventsPerDay = 0f,
                peakActivityDate = null,
                peakActivityCount = 0
            )
        }
        
        val sorted = events.sortedBy { it.date }
        val startDate = sorted.first().date
        val endDate = sorted.last().date
        val durationDays = TimeUnit.MILLISECONDS.toDays(endDate.time - startDate.time).toInt() + 1
        
        val eventsByType = events.groupBy { it.eventType }.mapValues { it.value.size }
        val averageEventsPerDay = if (durationDays > 0) events.size.toFloat() / durationDays else events.size.toFloat()
        
        // Find peak activity date
        val byDate = events.groupBy { event ->
            val calendar = Calendar.getInstance()
            calendar.time = event.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
        }
        val peakEntry = byDate.maxByOrNull { it.value.size }
        
        return TimelineSummary(
            totalEvents = events.size,
            startDate = startDate,
            endDate = endDate,
            durationDays = durationDays,
            eventsByType = eventsByType,
            averageEventsPerDay = averageEventsPerDay,
            peakActivityDate = peakEntry?.key,
            peakActivityCount = peakEntry?.value?.size ?: 0
        )
    }

    /**
     * Parse WhatsApp export format.
     */
    private fun parseWhatsAppMessages(evidence: Evidence, entities: List<Entity>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        val lines = evidence.extractedText.lines()
        
        for (line in lines) {
            val match = whatsappPattern.find(line) ?: continue
            
            val dateStr = match.groupValues[1]
            val sender = match.groupValues[2].trim()
            val message = match.groupValues[3].trim()
            
            val date = parseWhatsAppDate(dateStr) ?: continue
            val entityIds = findMatchingEntities(sender, entities)
            val eventType = classifyMessage(message)
            val significance = determineSignificance(message, eventType)
            
            events.add(TimelineEvent(
                date = date,
                description = "$sender: $message",
                sourceEvidenceId = evidence.id,
                entityIds = entityIds,
                eventType = eventType,
                significance = significance
            ))
        }
        
        return events
    }

    /**
     * Parse email into timeline events.
     */
    private fun parseEmailEvents(evidence: Evidence, entities: List<Entity>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        
        val date = evidence.metadata.creationDate ?: return events
        val sender = evidence.metadata.sender ?: "Unknown"
        val subject = evidence.metadata.subject ?: ""
        val entityIds = findMatchingEntities(sender, entities)
        
        events.add(TimelineEvent(
            date = date,
            description = "Email from $sender: $subject",
            sourceEvidenceId = evidence.id,
            entityIds = entityIds,
            eventType = EventType.COMMUNICATION,
            significance = Significance.NORMAL
        ))
        
        events.addAll(extractDatedEvents(evidence.extractedText, evidence.id, entities))
        
        return events
    }

    /**
     * Parse generic document for events.
     */
    private fun parseGenericEvents(evidence: Evidence, entities: List<Entity>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        
        evidence.metadata.creationDate?.let { date ->
            events.add(TimelineEvent(
                date = date,
                description = "Document created: ${evidence.fileName}",
                sourceEvidenceId = evidence.id,
                entityIds = emptyList(),
                eventType = EventType.DOCUMENT,
                significance = Significance.NORMAL
            ))
        }
        
        events.addAll(extractDatedEvents(evidence.extractedText, evidence.id, entities))
        
        return events
    }

    /**
     * Extract events with dates from text.
     */
    private fun extractDatedEvents(text: String, evidenceId: String, entities: List<Entity>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        val sentences = text.split(Regex("[.!?\\n]")).filter { it.isNotBlank() }
        
        for (sentence in sentences) {
            for ((format, pattern) in datePatterns) {
                val match = pattern.find(sentence) ?: continue
                val date = parseDate(match.value, format) ?: continue
                
                val eventType = classifyMessage(sentence)
                val significance = determineSignificance(sentence, eventType)
                val mentionedEntities = entities.filter { entity ->
                    sentence.contains(entity.primaryName, ignoreCase = true) ||
                    entity.aliases.any { sentence.contains(it, ignoreCase = true) }
                }.map { it.id }
                
                events.add(TimelineEvent(
                    date = date,
                    description = sentence.trim(),
                    sourceEvidenceId = evidenceId,
                    entityIds = mentionedEntities,
                    eventType = eventType,
                    significance = significance
                ))
                break
            }
        }
        
        return events
    }

    private fun createCluster(events: List<TimelineEvent>): EventCluster {
        val sortedEvents = events.sortedBy { it.date }
        val allEntityIds = events.flatMap { it.entityIds }.distinct()
        val eventTypes = events.map { it.eventType }.distinct()
        
        val significance = when {
            events.any { it.significance == Significance.CRITICAL } -> ClusterSignificance.CRITICAL
            events.count { it.significance == Significance.HIGH } >= 2 -> ClusterSignificance.HIGH
            events.size >= 5 -> ClusterSignificance.MEDIUM
            else -> ClusterSignificance.LOW
        }
        
        return EventCluster(
            id = UUID.randomUUID().toString(),
            startDate = sortedEvents.first().date,
            endDate = sortedEvents.last().date,
            eventCount = events.size,
            events = sortedEvents,
            involvedEntityIds = allEntityIds,
            eventTypes = eventTypes,
            significance = significance
        )
    }

    private fun classifyMessage(text: String): EventType {
        val lowerText = text.lowercase()
        
        return when {
            paymentKeywords.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(text) } -> EventType.PAYMENT
            promiseKeywords.any { lowerText.contains(it) } -> EventType.PROMISE
            denialKeywords.any { lowerText.contains(it) } -> EventType.DENIAL
            admissionKeywords.any { lowerText.contains(it) } -> EventType.ADMISSION
            else -> EventType.COMMUNICATION
        }
    }

    private fun determineSignificance(text: String, eventType: EventType): Significance {
        return when (eventType) {
            EventType.CONTRADICTION -> Significance.CRITICAL
            EventType.ADMISSION -> Significance.HIGH
            EventType.DENIAL -> Significance.HIGH
            EventType.PAYMENT -> Significance.HIGH
            EventType.PROMISE -> Significance.NORMAL
            else -> Significance.NORMAL
        }
    }

    private fun findMatchingEntities(name: String, entities: List<Entity>): List<String> {
        return entities.filter { entity ->
            entity.primaryName.equals(name, ignoreCase = true) ||
            entity.aliases.any { it.equals(name, ignoreCase = true) } ||
            entity.primaryName.contains(name, ignoreCase = true) ||
            name.contains(entity.primaryName, ignoreCase = true)
        }.map { it.id }
    }

    private fun parseWhatsAppDate(dateStr: String): Date? {
        val formats = listOf(
            "d/M/yy, h:mm a",
            "d/M/yy, HH:mm",
            "dd/MM/yyyy, HH:mm:ss",
            "dd/MM/yyyy, h:mm:ss a",
            "M/d/yy, h:mm a"
        )
        
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.US).parse(dateStr.trim())
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseDate(dateStr: String, format: String): Date? {
        return try {
            SimpleDateFormat(format, Locale.US).parse(dateStr.trim())
        } catch (_: Exception) {
            null
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }

    private fun calculatePatternConfidence(patternCount: Int, totalEvents: Int): Float {
        if (totalEvents == 0) return 0f
        return (patternCount.toFloat() / totalEvents).coerceAtMost(1f)
    }
}

// Data classes for timeline analysis

data class EventCluster(
    val id: String,
    val startDate: Date,
    val endDate: Date,
    val eventCount: Int,
    val events: List<TimelineEvent>,
    val involvedEntityIds: List<String>,
    val eventTypes: List<EventType>,
    val significance: ClusterSignificance
)

enum class ClusterSignificance {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class TimelineGap(
    val startDate: Date,
    val endDate: Date,
    val durationDays: Int,
    val isUnusual: Boolean,
    val precedingEventType: EventType,
    val followingEventType: EventType,
    val involvedEntityIds: List<String>
)

data class TimelinePattern(
    val patternType: PatternType,
    val description: String,
    val frequency: Int,
    val confidence: Float
)

enum class PatternType {
    WEEKLY, MONTHLY, SEASONAL, IRREGULAR
}

data class TimelineSummary(
    val totalEvents: Int,
    val startDate: Date?,
    val endDate: Date?,
    val durationDays: Int,
    val eventsByType: Map<EventType, Int>,
    val averageEventsPerDay: Float,
    val peakActivityDate: Date?,
    val peakActivityCount: Int
)
