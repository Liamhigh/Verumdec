package com.verumdec.forensic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.verumdec.contradiction.ContradictionEngine
import com.verumdec.timeline.TimelineEngine
import com.verumdec.image.ImageEngine
import com.verumdec.voice.VoiceEngine

class ForensicEngineFacade(
    private val contradictionEngine: ContradictionEngine,
    private val timelineEngine: TimelineEngine,
    private val imageEngine: ImageEngine,
    private val voiceEngine: VoiceEngine,
    private val sealer: FileSealer
) {
    suspend fun createCase(name: String): CaseResult = withContext(Dispatchers.IO) {
        val contradictions = contradictionEngine.scan(name)
        val timelineIssues = timelineEngine.analyze(name)
        val imageResult = imageEngine.scan(name)
        val voiceResult = voiceEngine.scan(name)
        val seal = sealer.generateSeal(name)
        
        CaseResult(
            contradictions = contradictions,
            timeline = timelineIssues,
            image = imageResult,
            voice = voiceResult,
            seal = seal
        )
    }
}

data class CaseResult(
    val contradictions: String,
    val timeline: String,
    val image: String,
    val voice: String,
    val seal: String
) : java.io.Serializable
