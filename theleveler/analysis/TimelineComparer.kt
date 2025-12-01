package ai.verum.theleveler.analysis

import ai.verum.theleveler.core.*
import ai.verum.theleveler.extraction.DateNormalizer

/**
 * Compares timeline events to detect chronological contradictions.
 * Detects impossible orderings and temporal inconsistencies.
 */
object TimelineComparer {

    // Temporal sequence indicators
    private val beforeIndicators = listOf(
        "before", "prior to", "earlier", "previously", "first",
        "initially", "originally", "at the start", "in the beginning"
    )

    private val afterIndicators = listOf(
        "after", "following", "later", "subsequently", "then",
        "afterwards", "next", "finally", "eventually", "in the end"
    )

    private val simultaneousIndicators = listOf(
        "while", "during", "at the same time", "simultaneously",
        "meanwhile", "as", "when", "at that moment"
    )

    /**
     * Compare timeline events and detect contradictions.
     */
    fun compareTimeline(events: List<TimelineEvent>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Group events by actor
        val byActor = events.filter { it.actor != null }.groupBy { it.actor!!.normalized }
        
        for ((_, actorEvents) in byActor) {
            if (actorEvents.size < 2) continue
            
            // Compare each pair of events
            for (i in actorEvents.indices) {
                for (j in i + 1 until actorEvents.size) {
                    val e1 = actorEvents[i]
                    val e2 = actorEvents[j]
                    
                    val contradiction = detectTimelineContradiction(e1, e2)
                    if (contradiction != null) {
                        contradictions.add(contradiction)
                    }
                }
            }
        }
        
        return contradictions
    }

    /**
     * Detect timeline contradiction between two events.
     */
    private fun detectTimelineContradiction(e1: TimelineEvent, e2: TimelineEvent): Contradiction? {
        val actor = e1.actor ?: return null
        
        // Check if timestamps exist and can be compared
        val ts1 = DateNormalizer.normalize(e1.timestamp)
        val ts2 = DateNormalizer.normalize(e2.timestamp)
        
        if (ts1 != null && ts2 != null) {
            // Compare normalized timestamps with language implications
            val timestampOrder = ts1.compareTo(ts2) // negative = e1 before e2
            val languageOrder = inferLanguageOrder(e1.description, e2.description)
            
            // Contradiction: timestamps say one thing, language says another
            if (timestampOrder < 0 && languageOrder > 0) {
                // Timestamps: e1 before e2, but language: e1 after e2
                return createTimelineContradiction(actor, e1, e2, 
                    "Timestamp indicates event 1 occurred before event 2, but language suggests the opposite order.")
            }
            
            if (timestampOrder > 0 && languageOrder < 0) {
                // Timestamps: e1 after e2, but language: e1 before e2
                return createTimelineContradiction(actor, e1, e2,
                    "Timestamp indicates event 1 occurred after event 2, but language suggests the opposite order.")
            }
        }
        
        // Check for impossible simultaneity
        if (claimsSimultaneous(e1.description, e2.description) && eventsIncompatible(e1, e2)) {
            return createTimelineContradiction(actor, e1, e2,
                "Events are claimed to occur simultaneously but appear to be incompatible.")
        }
        
        return null
    }

    /**
     * Infer temporal order from language.
     * Returns: negative if e1 described as before e2, positive if after, 0 if unclear
     */
    private fun inferLanguageOrder(desc1: String, desc2: String): Int {
        val d1Lower = desc1.lowercase()
        val d2Lower = desc2.lowercase()
        
        // Check if desc1 references desc2 as happening before/after
        for (indicator in beforeIndicators) {
            if (d1Lower.contains(indicator)) return 1  // e1 claims to be after something
        }
        
        for (indicator in afterIndicators) {
            if (d1Lower.contains(indicator)) return -1 // e1 claims to be before something
        }
        
        return 0 // No clear indication
    }

    /**
     * Check if descriptions claim events are simultaneous.
     */
    private fun claimsSimultaneous(desc1: String, desc2: String): Boolean {
        val combined = (desc1 + " " + desc2).lowercase()
        return simultaneousIndicators.any { combined.contains(it) }
    }

    /**
     * Check if two events are logically incompatible (can't happen at same time).
     */
    private fun eventsIncompatible(e1: TimelineEvent, e2: TimelineEvent): Boolean {
        val d1 = e1.description.lowercase()
        val d2 = e2.description.lowercase()
        
        // Location incompatibility
        val locationPatterns = listOf("at home", "at work", "at the office", "in the car", "at the hospital")
        val locations1 = locationPatterns.filter { d1.contains(it) }
        val locations2 = locationPatterns.filter { d2.contains(it) }
        
        if (locations1.isNotEmpty() && locations2.isNotEmpty() && 
            locations1.intersect(locations2.toSet()).isEmpty()) {
            return true
        }
        
        return false
    }

    /**
     * Create a timeline contradiction object.
     */
    private fun createTimelineContradiction(
        actor: Actor, 
        e1: TimelineEvent, 
        e2: TimelineEvent,
        explanation: String
    ): Contradiction {
        return Contradiction(
            actor = actor,
            firstStatement = Statement(actor, e1.description, e1.timestamp, e1.sourceId),
            secondStatement = Statement(actor, e2.description, e2.timestamp, e2.sourceId),
            type = ContradictionType.TIMELINE_CONTRADICTION,
            confidence = 0.75,
            explanation = "${actor.rawName}: $explanation"
        )
    }
}
