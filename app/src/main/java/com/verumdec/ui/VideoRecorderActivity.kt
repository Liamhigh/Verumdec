package com.verumdec.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.verumdec.databinding.ActivityVideoRecorderBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * VideoRecorderActivity - Record video using CameraX
 * 
 * Saves recorded video to:
 * /cases/{CASE_ID}/evidence/VID_xxx.mp4
 */
class VideoRecorderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoRecorderBinding
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var caseId: String? = null
    private lateinit var outputDirectory: File
    private var outputFile: File? = null

    companion object {
        const val EXTRA_CASE_ID = "case_id"
        private const val REQUEST_CODE_PERMISSIONS = 30
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoRecorderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        caseId = intent.getStringExtra(EXTRA_CASE_ID)
        if (caseId == null) {
            Toast.makeText(this, "Error: No case ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupOutputDirectory()

        // Request camera and audio permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnRecord.setOnClickListener { toggleRecording() }
        binding.btnCancel.setOnClickListener {
            recording?.stop()
            outputFile?.delete()
            finish()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupOutputDirectory() {
        val casesDir = File(getExternalFilesDir(null), "cases")
        val caseDir = File(casesDir, caseId!!)
        outputDirectory = File(caseDir, "evidence")
        outputDirectory.mkdirs()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // VideoCapture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )

            } catch (exc: Exception) {
                Toast.makeText(
                    this,
                    "Failed to start camera: ${exc.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleRecording() {
        if (recording != null) {
            // Stop recording
            recording?.stop()
            recording = null
            binding.btnRecord.text = "Start Recording"
            binding.statusText.text = "Recording saved"
            binding.recordingIndicator.visibility = android.view.View.GONE

            // Return file path to calling activity
            outputFile?.let { file ->
                val resultIntent = Intent().apply {
                    putExtra("file_path", file.absolutePath)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        } else {
            // Start recording
            startRecording()
        }
    }

    private fun startRecording() {
        val videoCapture = videoCapture ?: return

        // Create timestamped filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        outputFile = File(outputDirectory, "VID_$timestamp.mp4")

        val outputOptions = FileOutputOptions.Builder(outputFile!!).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                if (ContextCompat.checkSelfPermission(
                        this@VideoRecorderActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.btnRecord.text = "Stop Recording"
                        binding.statusText.text = "Recording..."
                        binding.recordingIndicator.visibility = android.view.View.VISIBLE
                        // Use ContextCompat for color compatibility
                        binding.btnRecord.setBackgroundColor(
                            ContextCompat.getColor(this@VideoRecorderActivity, android.R.color.holo_red_dark)
                        )
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Toast.makeText(
                                this,
                                "Video saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            recording?.close()
                            recording = null
                            Toast.makeText(
                                this,
                                "Video capture failed: ${recordEvent.error}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
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
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera and audio permissions required", 
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
