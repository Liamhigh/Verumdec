package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.AudioUtils
import com.verumdec.core.util.HashUtils
import java.io.File
import java.util.*

/**
 * AudioBrain - Analyzes audio evidence.
 * Handles voice recordings, calls, and audio messages.
 */
class AudioBrain(context: Context) : BaseBrain(context) {

    override val brainName = "AudioBrain"

    /**
     * Analyze audio file.
     */
    fun analyze(file: File): BrainResult<AudioAnalysis> {
        val hash = HashUtils.sha512(file)
        val metadata = mapOf(
            "fileName" to file.name,
            "sha512Hash" to hash,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(file, "ANALYZE_AUDIO", metadata) { audioFile ->
            val estimatedDuration = AudioUtils.estimateDuration(audioFile.length())
            
            AudioAnalysis(
                id = generateProcessingId(),
                fileName = audioFile.name,
                filePath = audioFile.absolutePath,
                contentHash = hash,
                fileSize = audioFile.length(),
                estimatedDurationMs = estimatedDuration,
                formattedDuration = AudioUtils.formatDuration(estimatedDuration),
                extension = audioFile.extension.lowercase(),
                isVoiceRecording = isLikelyVoiceRecording(audioFile.name),
                isPhoneCall = isLikelyPhoneCall(audioFile.name),
                qualityLabel = "Standard", // Would need actual metadata extraction
                analyzedAt = Date()
            )
        }
    }

    /**
     * Prepare audio for transcription.
     */
    fun prepareForTranscription(file: File): BrainResult<TranscriptionPreparation> {
        val metadata = mapOf(
            "fileName" to file.name,
            "sha512Hash" to HashUtils.sha512(file),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(file, "PREPARE_TRANSCRIPTION", metadata) { audioFile ->
            val isSupported = AudioUtils.isAudioFile(audioFile.name)
            val estimatedDuration = AudioUtils.estimateDuration(audioFile.length())

            TranscriptionPreparation(
                id = generateProcessingId(),
                originalFile = audioFile.absolutePath,
                isSupported = isSupported,
                estimatedDurationMs = estimatedDuration,
                estimatedTranscriptionTimeMs = estimatedDuration * 2, // Rough estimate
                preparedAt = Date()
            )
        }
    }

    /**
     * Analyze audio segments for speaker changes (simplified).
     */
    fun analyzeSegments(file: File, transcription: String?): BrainResult<AudioSegmentAnalysis> {
        val metadata = mapOf(
            "fileName" to file.name,
            "sha512Hash" to HashUtils.sha512(file),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(file, "ANALYZE_SEGMENTS", metadata) { _ ->
            val segments = if (transcription != null) {
                extractSpeakerSegments(transcription)
            } else {
                emptyList()
            }

            AudioSegmentAnalysis(
                id = generateProcessingId(),
                fileName = file.name,
                totalSegments = segments.size,
                speakerCount = segments.map { it.speaker }.distinct().size,
                segments = segments,
                analyzedAt = Date()
            )
        }
    }

    private fun isLikelyVoiceRecording(fileName: String): Boolean {
        val name = fileName.lowercase()
        return name.contains("voice") || name.contains("recording") ||
               name.contains("memo") || name.contains("note")
    }

    private fun isLikelyPhoneCall(fileName: String): Boolean {
        val name = fileName.lowercase()
        return name.contains("call") || name.contains("phone") ||
               name.contains("conversation")
    }

    private fun extractSpeakerSegments(transcription: String): List<AudioSegment> {
        val segments = mutableListOf<AudioSegment>()
        val pattern = Regex("^([^:]+):\\s*(.+)$", RegexOption.MULTILINE)
        
        pattern.findAll(transcription).forEach { match ->
            val speaker = match.groupValues[1].trim()
            val text = match.groupValues[2].trim()
            
            segments.add(AudioSegment(
                speaker = speaker,
                text = text,
                startMs = 0, // Would need actual timing
                endMs = 0
            ))
        }
        
        return segments
    }
}

data class AudioAnalysis(
    val id: String,
    val fileName: String,
    val filePath: String,
    val contentHash: String,
    val fileSize: Long,
    val estimatedDurationMs: Long,
    val formattedDuration: String,
    val extension: String,
    val isVoiceRecording: Boolean,
    val isPhoneCall: Boolean,
    val qualityLabel: String,
    val analyzedAt: Date
)

data class TranscriptionPreparation(
    val id: String,
    val originalFile: String,
    val isSupported: Boolean,
    val estimatedDurationMs: Long,
    val estimatedTranscriptionTimeMs: Long,
    val preparedAt: Date
)

data class AudioSegmentAnalysis(
    val id: String,
    val fileName: String,
    val totalSegments: Int,
    val speakerCount: Int,
    val segments: List<AudioSegment>,
    val analyzedAt: Date
)

data class AudioSegment(
    val speaker: String,
    val text: String,
    val startMs: Long,
    val endMs: Long
)
