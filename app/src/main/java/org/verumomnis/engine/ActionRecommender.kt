package org.verumomnis.engine

/**
 * STEP 11: RECOMMENDED ACTIONS
 * Based on the Verum Omnis template:
 * - RAKEZ → shareholder injunction (UAE Art. 110)
 * - SAPS → cybercrime device seizure
 * - Civil → damages claim
 */
class ActionRecommender {
    
    /**
     * Recommend actions based on top liabilities.
     * This step ALWAYS uses the Verum Omnis template.
     */
    fun recommendActions(topLiabilities: List<LiabilityEntry>): List<RecommendedAction> {
        val actions = mutableListOf<RecommendedAction>()
        
        for (liability in topLiabilities) {
            when (liability.category) {
                SubjectTag.SHAREHOLDER_OPPRESSION -> {
                    if (liability.totalSeverity > 0) {
                        actions.add(
                            RecommendedAction(
                                authority = "RAKEZ",
                                action = "File for shareholder injunction",
                                legalBasis = "UAE Commercial Companies Law Article 110 - Protection against oppressive conduct"
                            )
                        )
                    }
                }
                
                SubjectTag.CYBERCRIME -> {
                    if (liability.totalSeverity > 0) {
                        actions.add(
                            RecommendedAction(
                                authority = "SAPS (South African Police Service)",
                                action = "Request cybercrime device seizure and forensic analysis",
                                legalBasis = "Cybercrimes Act 19 of 2020 - Unauthorized access and data interference"
                            )
                        )
                    }
                }
                
                SubjectTag.BREACH_OF_FIDUCIARY_DUTY -> {
                    if (liability.totalSeverity > 0) {
                        actions.add(
                            RecommendedAction(
                                authority = "Civil Court",
                                action = "File damages claim for breach of fiduciary duty",
                                legalBasis = "Common law fiduciary duties - Duty of loyalty and duty of care"
                            )
                        )
                    }
                }
                
                SubjectTag.FRAUDULENT_EVIDENCE -> {
                    if (liability.totalSeverity > 0) {
                        actions.add(
                            RecommendedAction(
                                authority = "Civil Court",
                                action = "Pursue damages claim for fraud and misrepresentation",
                                legalBasis = "Common law fraud - Material misrepresentation with intent to deceive"
                            )
                        )
                    }
                }
                
                SubjectTag.EMOTIONAL_EXPLOITATION -> {
                    if (liability.totalSeverity > 0) {
                        actions.add(
                            RecommendedAction(
                                authority = "Civil Court",
                                action = "File damages claim for emotional distress and manipulation",
                                legalBasis = "Tort law - Intentional infliction of emotional distress"
                            )
                        )
                    }
                }
            }
        }
        
        return actions
    }
}
