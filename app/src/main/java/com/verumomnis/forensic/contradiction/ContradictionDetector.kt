package com.verumomnis.forensic.contradiction

/**
 * Detects contradictions between claims.
 * This is the core truth engine of Verum Omnis.
 */
object ContradictionDetector {

    /**
     * Check a new claim against historical claims for contradictions.
     */
    fun check(newClaim: Claim, history: List<Claim>): List<String> {
        val contradictions = mutableListOf<String>()

        history.forEach { old ->
            // Check for direct contradictions from the same speaker
            if (old.speaker == newClaim.speaker &&
                old.claimType == newClaim.claimType &&
                old.entities.intersect(newClaim.entities.toSet()).isNotEmpty()
            ) {
                // Denial followed by admission
                if (old.claimType == "denial" && newClaim.claimType != "denial") {
                    contradictions.add(
                        "Direct contradiction: Previously denied ('${old.content.take(50)}...') now claims otherwise"
                    )
                }
                
                // Check for contradicting statements about same entity
                if (old.content.contains("never", true) &&
                    newClaim.content.contains(old.entities.firstOrNull() ?: "", true)
                ) {
                    contradictions.add(
                        "Direct contradiction with previous claim: '${old.content.take(80)}...'"
                    )
                }
            }

            // Cross-reference contradiction
            if (old.speaker != newClaim.speaker) {
                val commonEntities = old.entities.intersect(newClaim.entities.toSet())
                if (commonEntities.isNotEmpty() && old.claimType != newClaim.claimType) {
                    contradictions.add(
                        "Cross-reference contradiction about ${commonEntities.first()}: " +
                        "Claim types differ (${old.claimType} vs ${newClaim.claimType})"
                    )
                }
            }
        }

        return contradictions
    }

    /**
     * Score the severity of contradictions.
     */
    fun scoreSeverity(contradictions: List<String>): String {
        return when {
            contradictions.isEmpty() -> "none"
            contradictions.size == 1 -> "low"
            contradictions.size <= 3 -> "medium"
            contradictions.size <= 5 -> "high"
            else -> "critical"
        }
    }
}
