package com.verumdec.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.verumdec.databinding.ActivityAudioRecorderBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * AudioRecorderActivity - Record audio using MediaRecorder
 * 
 * Saves recorded audio to:
 * /cases/{CASE_ID}/evidence/AUD_xxx.m4a
 */
class AudioRecorderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioRecorderBinding
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var caseId: String? = null
    private var isRecording = false
    private lateinit var outputDirectory: File

    companion object {
        const val EXTRA_CASE_ID = "case_id"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecorderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        caseId = intent.getStringExtra(EXTRA_CASE_ID)
        if (caseId == null) {
            Toast.makeText(this, "Error: No case ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupOutputDirectory()
        setupUI()

        // Request audio permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setupOutputDirectory() {
        val casesDir = File(getExternalFilesDir(null), "cases")
        val caseDir = File(casesDir, caseId!!)
        outputDirectory = File(caseDir, "evidence")
        outputDirectory.mkdirs()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Record Audio"

        binding.btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.btnCancel.setOnClickListener {
            if (isRecording) {
                stopRecording()
                // Delete the file
                outputFile?.delete()
            }
            finish()
        }

        binding.btnDone.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            finishWithResult()
        }

        updateUI()
    }

    private fun startRecording() {
        if (!allPermissionsGranted()) {
            Toast.makeText(this, "Audio permission required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create timestamped filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis())
            outputFile = File(outputDirectory, "AUD_$timestamp.m4a")

            // Setup MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecording = true
                    binding.chronometer.base = SystemClock.elapsedRealtime()
                    binding.chronometer.start()
                    updateUI()
                } catch (e: IOException) {
                    Toast.makeText(
                        this@AudioRecorderActivity,
                        "Failed to start recording: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                binding.chronometer.stop()
                updateUI()
                
                Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error stopping recording: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun finishWithResult() {
        if (outputFile != null && outputFile!!.exists()) {
            val resultIntent = Intent().apply {
                putExtra("file_path", outputFile!!.absolutePath)
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun updateUI() {
        if (isRecording) {
            binding.btnRecord.text = "Stop Recording"
            binding.btnRecord.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            binding.btnDone.isEnabled = false
            binding.statusText.text = "Recording..."
            binding.recordingIndicator.visibility = android.view.View.VISIBLE
        } else {
            binding.btnRecord.text = "Start Recording"
            binding.btnRecord.setBackgroundColor(getColor(android.R.color.holo_red_light))
            binding.btnDone.isEnabled = outputFile != null
            binding.statusText.text = if (outputFile != null) "Recording ready" else "Ready to record"
            binding.recordingIndicator.visibility = android.view.View.GONE
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Audio permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
