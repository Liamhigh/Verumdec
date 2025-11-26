package com.verumomnis.forensic.contradiction

/**
 * Timeline Brain - Detects impossible event sequences.
 * Checks for temporal inconsistencies in claims.
 */
object TimelineBrain {

    /**
     * Check if two events are temporally impossible
     * (e.g., claim to be in two distant places at similar times)
     */
    fun impossibleEvent(a: Claim, b: Claim): Boolean {
        // Future upgrade: compare distances, timestamps, GPS metadata
        // For now, check for obvious temporal conflicts
        
        val aTimeRefs = a.timeRefs
        val bTimeRefs = b.timeRefs
        
        // If both claims reference the same time but contradict each other
        if (aTimeRefs.intersect(bTimeRefs.toSet()).isNotEmpty()) {
            if (a.claimType == "denial" && b.claimType != "denial") {
                return true
            }
        }
        
        return false
    }

    /**
     * Build a chronological timeline from claims.
     */
    fun buildTimeline(claims: List<Claim>): List<Pair<String, Claim>> {
        val timeline = mutableListOf<Pair<String, Claim>>()
        
        claims.forEach { claim ->
            claim.timeRefs.forEach { timeRef ->
                timeline.add(timeRef to claim)
            }
        }
        
        return timeline.sortedBy { it.first }
    }
}
