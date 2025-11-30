package com.verumdec.engine

import com.verumdec.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Timeline Generator
 * Builds chronological timelines from evidence.
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
        
        // Main email event
        events.add(TimelineEvent(
            date = date,
            description = "Email from $sender: $subject",
            sourceEvidenceId = evidence.id,
            entityIds = entityIds,
            eventType = EventType.COMMUNICATION,
            significance = Significance.NORMAL
        ))
        
        // Extract dated events from body
        events.addAll(extractDatedEvents(evidence.extractedText, evidence.id, entities))
        
        return events
    }

    /**
     * Parse generic document for events.
     */
    private fun parseGenericEvents(evidence: Evidence, entities: List<Entity>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        
        // Add document creation event
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
        
        // Extract dated events from text
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

    /**
     * Classify a message/sentence by type.
     */
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

    /**
     * Determine significance based on content.
     */
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

    /**
     * Find entities matching a name.
     */
    private fun findMatchingEntities(name: String, entities: List<Entity>): List<String> {
        return entities.filter { entity ->
            entity.primaryName.equals(name, ignoreCase = true) ||
            entity.aliases.any { it.equals(name, ignoreCase = true) } ||
            entity.primaryName.contains(name, ignoreCase = true) ||
            name.contains(entity.primaryName, ignoreCase = true)
        }.map { it.id }
    }

    /**
     * Parse WhatsApp date format.
     */
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

    /**
     * Parse date with specific format.
     */
    private fun parseDate(dateStr: String, format: String): Date? {
        return try {
            SimpleDateFormat(format, Locale.US).parse(dateStr.trim())
        } catch (_: Exception) {
            null
        }
    }
    
    /**
     * Auto-build timeline with intelligent gap filling and relationship detection.
     * This enhanced method provides better timeline coherence.
     */
    fun autoBuiltTimeline(
        evidenceList: List<Evidence>,
        entities: List<Entity>,
        options: TimelineOptions = TimelineOptions()
    ): TimelineResult {
        val rawEvents = generateTimeline(evidenceList, entities)
        
        // Sort and deduplicate
        val sortedEvents = rawEvents.sortedBy { it.date }.distinctBy { 
            "${it.date.time / 60000}-${it.eventType}-${it.description.take(50)}" 
        }
        
        // Analyze gaps
        val gaps = analyzeTimelineGaps(sortedEvents)
        
        // Detect clusters of activity
        val clusters = detectActivityClusters(sortedEvents)
        
        // Build entity timelines
        val entityTimelines = buildEntityTimelines(sortedEvents, entities)
        
        // Generate timeline summary
        val summary = generateTimelineSummary(sortedEvents, gaps, clusters)
        
        return TimelineResult(
            events = if (options.maxEvents > 0) sortedEvents.take(options.maxEvents) else sortedEvents,
            gaps = gaps,
            clusters = clusters,
            entityTimelines = entityTimelines,
            summary = summary
        )
    }
    
    /**
     * Analyze gaps in the timeline.
     */
    private fun analyzeTimelineGaps(events: List<TimelineEvent>): List<TimelineGap> {
        val gaps = mutableListOf<TimelineGap>()
        
        if (events.size < 2) return gaps
        
        for (i in 0 until events.size - 1) {
            val current = events[i]
            val next = events[i + 1]
            val gapDays = (next.date.time - current.date.time) / (24 * 60 * 60 * 1000)
            
            if (gapDays >= 7) { // Gap of 7+ days is significant
                gaps.add(TimelineGap(
                    startDate = current.date,
                    endDate = next.date,
                    daysLength = gapDays.toInt(),
                    significance = when {
                        gapDays >= 30 -> Significance.CRITICAL
                        gapDays >= 14 -> Significance.HIGH
                        else -> Significance.NORMAL
                    }
                ))
            }
        }
        
        return gaps
    }
    
    /**
     * Detect clusters of activity in the timeline.
     */
    private fun detectActivityClusters(events: List<TimelineEvent>): List<ActivityCluster> {
        val clusters = mutableListOf<ActivityCluster>()
        
        if (events.isEmpty()) return clusters
        
        var clusterStart = events.first().date
        var clusterEvents = mutableListOf<TimelineEvent>()
        
        for (event in events) {
            val daysSinceClusterStart = (event.date.time - clusterStart.time) / (24 * 60 * 60 * 1000)
            
            if (daysSinceClusterStart <= 3) {
                clusterEvents.add(event)
            } else {
                if (clusterEvents.size >= 3) {
                    clusters.add(ActivityCluster(
                        startDate = clusterStart,
                        endDate = clusterEvents.last().date,
                        eventCount = clusterEvents.size,
                        dominantType = clusterEvents.groupBy { it.eventType }
                            .maxByOrNull { it.value.size }?.key ?: EventType.OTHER
                    ))
                }
                clusterStart = event.date
                clusterEvents = mutableListOf(event)
            }
        }
        
        // Add final cluster if significant
        if (clusterEvents.size >= 3) {
            clusters.add(ActivityCluster(
                startDate = clusterStart,
                endDate = clusterEvents.last().date,
                eventCount = clusterEvents.size,
                dominantType = clusterEvents.groupBy { it.eventType }
                    .maxByOrNull { it.value.size }?.key ?: EventType.OTHER
            ))
        }
        
        return clusters
    }
    
    /**
     * Build separate timelines for each entity.
     */
    private fun buildEntityTimelines(
        events: List<TimelineEvent>,
        entities: List<Entity>
    ): Map<String, List<TimelineEvent>> {
        return entities.associate { entity ->
            entity.id to events.filter { event ->
                entity.id in event.entityIds ||
                event.description.contains(entity.primaryName, ignoreCase = true) ||
                entity.aliases.any { alias -> 
                    event.description.contains(alias, ignoreCase = true) 
                }
            }
        }
    }
    
    /**
     * Generate a summary of the timeline.
     */
    private fun generateTimelineSummary(
        events: List<TimelineEvent>,
        gaps: List<TimelineGap>,
        clusters: List<ActivityCluster>
    ): TimelineSummary {
        if (events.isEmpty()) {
            return TimelineSummary(
                totalEvents = 0,
                dateRange = null,
                significantGaps = 0,
                activityClusters = 0,
                eventTypeBreakdown = emptyMap()
            )
        }
        
        return TimelineSummary(
            totalEvents = events.size,
            dateRange = Pair(events.first().date, events.last().date),
            significantGaps = gaps.count { it.significance == Significance.HIGH || it.significance == Significance.CRITICAL },
            activityClusters = clusters.size,
            eventTypeBreakdown = events.groupBy { it.eventType }
                .mapValues { it.value.size }
        )
    }
}

/**
 * Timeline generation options.
 */
data class TimelineOptions(
    val maxEvents: Int = 0,  // 0 = no limit
    val includeGapAnalysis: Boolean = true,
    val includeClusterDetection: Boolean = true
)

/**
 * Result of enhanced timeline generation.
 */
data class TimelineResult(
    val events: List<TimelineEvent>,
    val gaps: List<TimelineGap>,
    val clusters: List<ActivityCluster>,
    val entityTimelines: Map<String, List<TimelineEvent>>,
    val summary: TimelineSummary
)

/**
 * Represents a gap in the timeline.
 */
data class TimelineGap(
    val startDate: Date,
    val endDate: Date,
    val daysLength: Int,
    val significance: Significance
)

/**
 * Represents a cluster of activity.
 */
data class ActivityCluster(
    val startDate: Date,
    val endDate: Date,
    val eventCount: Int,
    val dominantType: EventType
)

/**
 * Summary of timeline analysis.
 */
data class TimelineSummary(
    val totalEvents: Int,
    val dateRange: Pair<Date, Date>?,
    val significantGaps: Int,
    val activityClusters: Int,
    val eventTypeBreakdown: Map<EventType, Int>
)
