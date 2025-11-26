package com.verumomnis.forensic.contradiction

import com.verumomnis.forensic.nlp.NLPUtil

/**
 * Extracts structured claims from raw text input.
 */
object ClaimExtractor {

    fun extract(text: String): Claim {
        val id = "stmt_" + System.currentTimeMillis()

        return Claim(
            id = id,
            speaker = "user",
            content = text,
            entities = NLPUtil.extractEntities(text),
            timeRefs = NLPUtil.extractDates(text),
            claimType = NLPUtil.classifyClaim(text)
        )
    }
}
