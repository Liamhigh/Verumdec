package com.verumomnis.forensic.contradiction

/**
 * Represents a factual claim made by a speaker.
 */
data class Claim(
    val id: String,
    val speaker: String,
    val content: String,
    val entities: List<String>,
    val timeRefs: List<String>,
    val claimType: String
)
