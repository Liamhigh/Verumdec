package ai.verum.theleveler.extraction

import ai.verum.theleveler.core.Actor
import ai.verum.theleveler.core.Statement

/**
 * Extracts statements from raw text, associating each with an actor.
 */
object StatementExtractor {

    fun extract(text: String, defaultActor: Actor? = null): List<Statement> {
        val normalized = TextNormalizer.normalize(text)
        val lines = normalized.split("\n").filter { it.isNotBlank() }
        
        val statements = mutableListOf<Statement>()
        var currentActor = defaultActor
        
        for (line in lines) {
            val (extractedActor, content) = SpeakerExtractor.extractSpeaker(line)
            if (extractedActor != null) {
                currentActor = extractedActor
            }
            
            if (content.isNotBlank() && currentActor != null) {
                val sentences = SentenceTokenizer.tokenize(content)
                for (sentence in sentences) {
                    statements.add(Statement(actor = currentActor, text = sentence))
                }
            }
        }
        
        return statements
    }
}
