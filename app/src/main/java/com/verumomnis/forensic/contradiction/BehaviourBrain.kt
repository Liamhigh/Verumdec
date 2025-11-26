package com.verumomnis.forensic.contradiction

/**
 * Behaviour Brain - Detects stress markers, manipulation patterns,
 * and deceptive language patterns.
 */
object BehaviourBrain {

    /**
     * Detect stress and evasion markers in text.
     */
    fun detectStressMarkers(text: String): List<String> {
        val flags = mutableListOf<String>()
        val lowerText = text.lowercase()

        // Avoidance patterns
        if (lowerText.contains("i don't remember")) {
            flags.add("Avoidance marker: 'I don't remember'")
        }
        if (lowerText.contains("i can't recall")) {
            flags.add("Avoidance marker: 'I can't recall'")
        }
        if (lowerText.contains("i forgot")) {
            flags.add("Avoidance marker: 'I forgot'")
        }

        // Defensive language
        if (lowerText.contains("why are you asking")) {
            flags.add("Defensive language spike")
        }
        if (lowerText.contains("why do you need to know")) {
            flags.add("Defensive language spike")
        }
        if (lowerText.contains("none of your business")) {
            flags.add("Hostile deflection detected")
        }

        // Gaslighting patterns
        if (lowerText.contains("you're crazy")) {
            flags.add("Gaslighting pattern: 'you're crazy'")
        }
        if (lowerText.contains("you're imagining")) {
            flags.add("Gaslighting pattern: 'you're imagining'")
        }
        if (lowerText.contains("that never happened")) {
            flags.add("Gaslighting pattern: denial of events")
        }
        if (lowerText.contains("you're overreacting")) {
            flags.add("Gaslighting pattern: minimization")
        }

        // Blame shifting
        if (lowerText.contains("it's your fault")) {
            flags.add("Blame shifting detected")
        }
        if (lowerText.contains("you made me")) {
            flags.add("Blame shifting: victim blaming")
        }

        // Over-explaining (fraud red flag)
        if (text.length > 500 && text.count { it == ',' } > 10) {
            flags.add("Over-explaining detected (classic fraud indicator)")
        }

        // Passive admissions
        if (lowerText.contains("i thought i was in the clear")) {
            flags.add("Passive admission detected")
        }
        if (lowerText.contains("i didn't think anyone would notice")) {
            flags.add("Passive admission: concealment intent")
        }

        // Pressure tactics
        if (lowerText.contains("you need to") || lowerText.contains("you must")) {
            flags.add("Pressure tactic detected")
        }
        if (lowerText.contains("or else")) {
            flags.add("Threat/ultimatum detected")
        }

        return flags
    }

    /**
     * Detect manipulation patterns.
     */
    fun detectManipulation(text: String): List<String> {
        val flags = mutableListOf<String>()
        val lowerText = text.lowercase()

        // Financial manipulation
        if (lowerText.contains("just send") && lowerText.contains("money")) {
            flags.add("Financial manipulation: urgent money request")
        }
        if (lowerText.contains("invest") && lowerText.contains("guarantee")) {
            flags.add("Financial manipulation: guaranteed returns claim")
        }

        // Emotional manipulation
        if (lowerText.contains("if you loved me")) {
            flags.add("Emotional manipulation: conditional love")
        }
        if (lowerText.contains("after everything i've done")) {
            flags.add("Emotional manipulation: guilt tripping")
        }

        // Authority manipulation
        if (lowerText.contains("trust me") || lowerText.contains("believe me")) {
            flags.add("Trust assertion without evidence")
        }

        return flags
    }
}
