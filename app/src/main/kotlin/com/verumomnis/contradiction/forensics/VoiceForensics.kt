package com.verumomnis.contradiction.forensics

import kotlin.math.*

/**
 * Voice Forensics Module
 *
 * Provides offline audio forensic analysis capabilities:
 * - MFCC (Mel-Frequency Cepstral Coefficients) extraction
 * - Spectral analysis
 * - Voice pattern matching
 * - Audio tampering detection
 */
class VoiceForensics {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SIZE = 512
        private const val HOP_SIZE = 160
        private const val NUM_MEL_FILTERS = 26
        private const val NUM_MFCC = 13
    }

    /**
     * Performs complete voice forensic analysis on audio samples.
     */
    fun analyzeAudio(audioSamples: FloatArray): VoiceForensicResult {
        if (audioSamples.isEmpty()) {
            return VoiceForensicResult(
                mfccFeatures = emptyList(),
                spectralFeatures = SpectralFeatures(0.0, 0.0, 0.0, 0.0),
                voiceActivitySegments = emptyList(),
                tamperingScore = 0f,
                isTampered = false
            )
        }

        val mfccFeatures = extractMFCC(audioSamples)
        val spectralFeatures = analyzeSpectrum(audioSamples)
        val voiceActivity = detectVoiceActivity(audioSamples)
        val tamperingScore = detectTampering(audioSamples, spectralFeatures)

        return VoiceForensicResult(
            mfccFeatures = mfccFeatures,
            spectralFeatures = spectralFeatures,
            voiceActivitySegments = voiceActivity,
            tamperingScore = tamperingScore,
            isTampered = tamperingScore > 0.5f
        )
    }

    /**
     * Extracts MFCC features from audio frames.
     * MFCCs are commonly used for speaker identification and voice analysis.
     */
    private fun extractMFCC(samples: FloatArray): List<FloatArray> {
        val mfccFrames = mutableListOf<FloatArray>()
        val numFrames = (samples.size - FRAME_SIZE) / HOP_SIZE

        for (frameIdx in 0 until numFrames) {
            val startIdx = frameIdx * HOP_SIZE
            val frame = samples.sliceArray(startIdx until startIdx + FRAME_SIZE)

            // Apply Hamming window
            val windowedFrame = applyHammingWindow(frame)

            // Compute power spectrum
            val powerSpectrum = computePowerSpectrum(windowedFrame)

            // Apply mel filterbank
            val melSpectrum = applyMelFilterbank(powerSpectrum)

            // Apply log and DCT to get MFCCs
            val mfcc = computeDCT(melSpectrum.map { ln(it + 1e-10) }.toFloatArray())

            mfccFrames.add(mfcc.sliceArray(0 until NUM_MFCC))
        }

        return mfccFrames
    }

    /**
     * Applies Hamming window to audio frame.
     */
    private fun applyHammingWindow(frame: FloatArray): FloatArray {
        return FloatArray(frame.size) { i ->
            val window = 0.54f - 0.46f * cos(2.0 * PI * i / (frame.size - 1)).toFloat()
            frame[i] * window
        }
    }

    /**
     * Computes power spectrum using simplified FFT approximation.
     */
    private fun computePowerSpectrum(frame: FloatArray): FloatArray {
        val n = frame.size
        val spectrum = FloatArray(n / 2)

        for (k in 0 until n / 2) {
            var real = 0f
            var imag = 0f
            for (t in 0 until n) {
                val angle = 2.0 * PI * k * t / n
                real += frame[t] * cos(angle).toFloat()
                imag -= frame[t] * sin(angle).toFloat()
            }
            spectrum[k] = (real * real + imag * imag) / n
        }

        return spectrum
    }

    /**
     * Applies mel filterbank to power spectrum.
     */
    private fun applyMelFilterbank(powerSpectrum: FloatArray): FloatArray {
        val melEnergies = FloatArray(NUM_MEL_FILTERS)
        val numBins = powerSpectrum.size

        for (i in 0 until NUM_MEL_FILTERS) {
            // Simplified mel filter calculation
            val lowerBin = (i.toFloat() / NUM_MEL_FILTERS * numBins * 0.8).toInt()
            val upperBin = ((i + 2).toFloat() / NUM_MEL_FILTERS * numBins * 0.8).toInt().coerceAtMost(numBins - 1)

            for (j in lowerBin..upperBin) {
                val weight = if (j < (lowerBin + upperBin) / 2) {
                    (j - lowerBin).toFloat() / ((upperBin - lowerBin) / 2 + 1)
                } else {
                    (upperBin - j).toFloat() / ((upperBin - lowerBin) / 2 + 1)
                }
                melEnergies[i] += powerSpectrum[j] * weight.coerceAtLeast(0f)
            }
        }

        return melEnergies
    }

    /**
     * Computes DCT (Discrete Cosine Transform) for MFCC.
     */
    private fun computeDCT(input: FloatArray): FloatArray {
        val n = input.size
        val output = FloatArray(n)

        for (k in 0 until n) {
            var sum = 0f
            for (i in 0 until n) {
                sum += input[i] * cos(PI * k * (2 * i + 1) / (2 * n)).toFloat()
            }
            output[k] = sum * sqrt(2.0 / n).toFloat()
        }

        return output
    }

    /**
     * Analyzes spectral characteristics of audio.
     */
    private fun analyzeSpectrum(samples: FloatArray): SpectralFeatures {
        // Compute overall spectrum
        val paddedLength = samples.size.takeHighestOneBit() * 2
        val paddedSamples = FloatArray(paddedLength)
        samples.copyInto(paddedSamples)

        val spectrum = computePowerSpectrum(paddedSamples)

        // Calculate spectral centroid
        var numerator = 0.0
        var denominator = 0.0
        for (i in spectrum.indices) {
            val freq = i.toDouble() * SAMPLE_RATE / paddedLength
            numerator += freq * spectrum[i]
            denominator += spectrum[i]
        }
        val centroid = if (denominator > 0) numerator / denominator else 0.0

        // Calculate spectral bandwidth
        var bandwidthSum = 0.0
        for (i in spectrum.indices) {
            val freq = i.toDouble() * SAMPLE_RATE / paddedLength
            bandwidthSum += (freq - centroid).pow(2) * spectrum[i]
        }
        val bandwidth = if (denominator > 0) sqrt(bandwidthSum / denominator) else 0.0

        // Calculate spectral rolloff (85% energy)
        val totalEnergy = spectrum.sum()
        var cumulativeEnergy = 0f
        var rolloff = 0.0
        for (i in spectrum.indices) {
            cumulativeEnergy += spectrum[i]
            if (cumulativeEnergy >= 0.85 * totalEnergy) {
                rolloff = i.toDouble() * SAMPLE_RATE / paddedLength
                break
            }
        }

        // Calculate spectral flatness (geometric mean / arithmetic mean)
        val geometricMean = exp(spectrum.map { ln((it + 1e-10).toDouble()) }.average())
        val arithmeticMean = spectrum.average()
        val flatness = if (arithmeticMean > 0) geometricMean / arithmeticMean else 0.0

        return SpectralFeatures(
            centroid = centroid,
            bandwidth = bandwidth,
            rolloff = rolloff,
            flatness = flatness
        )
    }

    /**
     * Detects voice activity segments in audio.
     */
    private fun detectVoiceActivity(samples: FloatArray): List<VoiceSegment> {
        val segments = mutableListOf<VoiceSegment>()
        val frameEnergies = mutableListOf<Float>()

        // Calculate frame energies
        val numFrames = samples.size / FRAME_SIZE
        for (i in 0 until numFrames) {
            val start = i * FRAME_SIZE
            val end = minOf(start + FRAME_SIZE, samples.size)
            val frame = samples.sliceArray(start until end)
            val energy = frame.map { it * it }.sum() / frame.size
            frameEnergies.add(energy)
        }

        if (frameEnergies.isEmpty()) return segments

        // Dynamic threshold based on energy distribution
        val sortedEnergies = frameEnergies.sorted()
        val threshold = sortedEnergies[sortedEnergies.size / 4] * 2

        // Detect segments
        var inSegment = false
        var segmentStart = 0

        for (i in frameEnergies.indices) {
            if (frameEnergies[i] > threshold) {
                if (!inSegment) {
                    segmentStart = i * FRAME_SIZE
                    inSegment = true
                }
            } else {
                if (inSegment) {
                    val segmentEnd = i * FRAME_SIZE
                    val duration = (segmentEnd - segmentStart).toFloat() / SAMPLE_RATE
                    if (duration > 0.1f) { // Minimum 100ms
                        segments.add(
                            VoiceSegment(
                                startTime = segmentStart.toFloat() / SAMPLE_RATE,
                                endTime = segmentEnd.toFloat() / SAMPLE_RATE,
                                averageEnergy = frameEnergies.subList(
                                    segmentStart / FRAME_SIZE,
                                    minOf(i, frameEnergies.size)
                                ).average().toFloat()
                            )
                        )
                    }
                    inSegment = false
                }
            }
        }

        return segments
    }

    /**
     * Detects audio tampering based on spectral discontinuities.
     */
    private fun detectTampering(samples: FloatArray, spectral: SpectralFeatures): Float {
        var tamperingScore = 0f

        // Check for unnatural spectral characteristics
        if (spectral.flatness > 0.8) {
            // Very flat spectrum is suspicious (may indicate noise generation)
            tamperingScore += 0.3f
        }

        if (spectral.centroid > 8000 || spectral.centroid < 200) {
            // Extreme spectral centroid is unusual for speech
            tamperingScore += 0.2f
        }

        // Check for discontinuities in the signal
        val discontinuities = detectDiscontinuities(samples)
        tamperingScore += 0.5f * minOf(discontinuities.toFloat() / 10f, 1f)

        return tamperingScore.coerceIn(0f, 1f)
    }

    /**
     * Detects sharp discontinuities in audio that may indicate splicing.
     */
    private fun detectDiscontinuities(samples: FloatArray): Int {
        var count = 0
        val threshold = samples.map { abs(it) }.average() * 5

        for (i in 1 until samples.size) {
            if (abs(samples[i] - samples[i - 1]) > threshold) {
                count++
            }
        }

        return count
    }

    /**
     * Compares two voice samples for speaker similarity.
     */
    fun compareSpeakers(sample1: FloatArray, sample2: FloatArray): Float {
        val mfcc1 = extractMFCC(sample1)
        val mfcc2 = extractMFCC(sample2)

        if (mfcc1.isEmpty() || mfcc2.isEmpty()) return 0f

        // Calculate average MFCC for each sample
        val avgMfcc1 = FloatArray(NUM_MFCC)
        val avgMfcc2 = FloatArray(NUM_MFCC)

        mfcc1.forEach { frame -> frame.forEachIndexed { i, v -> avgMfcc1[i] += v } }
        mfcc2.forEach { frame -> frame.forEachIndexed { i, v -> avgMfcc2[i] += v } }

        avgMfcc1.forEachIndexed { i, _ -> avgMfcc1[i] /= mfcc1.size }
        avgMfcc2.forEachIndexed { i, _ -> avgMfcc2[i] /= mfcc2.size }

        // Calculate cosine similarity
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in 0 until NUM_MFCC) {
            dotProduct += avgMfcc1[i] * avgMfcc2[i]
            norm1 += avgMfcc1[i] * avgMfcc1[i]
            norm2 += avgMfcc2[i] * avgMfcc2[i]
        }

        return if (norm1 > 0 && norm2 > 0) {
            (dotProduct / (sqrt(norm1) * sqrt(norm2))).coerceIn(0f, 1f)
        } else 0f
    }
}

/**
 * Complete voice forensic analysis result.
 */
data class VoiceForensicResult(
    val mfccFeatures: List<FloatArray>,
    val spectralFeatures: SpectralFeatures,
    val voiceActivitySegments: List<VoiceSegment>,
    val tamperingScore: Float,
    val isTampered: Boolean
)

/**
 * Spectral analysis features.
 */
data class SpectralFeatures(
    val centroid: Double,   // Frequency center of mass
    val bandwidth: Double,  // Spectral spread
    val rolloff: Double,    // Frequency below which 85% of energy lies
    val flatness: Double    // Measure of how flat/noisy the spectrum is
)

/**
 * Voice activity segment.
 */
data class VoiceSegment(
    val startTime: Float,   // Start time in seconds
    val endTime: Float,     // End time in seconds
    val averageEnergy: Float // Average energy in segment
)
