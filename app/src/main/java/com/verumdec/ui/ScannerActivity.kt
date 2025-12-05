package com.verumdec.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.verumdec.databinding.ActivityScannerBinding
import com.verumdec.engine.ForensicEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scanner Activity for capturing and adding evidence.
 * Allows users to take photos or select files to add to a case.
 */
class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var forensicEngine: ForensicEngine
    private var caseId: String? = null
    private var capturedBitmap: Bitmap? = null
    private var selectedUri: Uri? = null
    private var selectedFileName: String? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                capturedBitmap = it
                displayPreview(it)
            }
        }
    }

    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedFileName = getFileName(it)
            displayFilePreview(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val inflater = LayoutInflater.from(this)
        binding = ActivityScannerBinding.inflate(inflater)
        setContentView(binding.root)

        forensicEngine = ForensicEngine(this)
        caseId = intent.getStringExtra("caseId")

        if (caseId == null) {
            Toast.makeText(this, "Error: No case ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnSelectFile.setOnClickListener {
            selectFile()
        }

        binding.btnSaveEvidence.setOnClickListener {
            saveEvidence()
        }
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun selectFile() {
        selectFileLauncher.launch(arrayOf(
            "application/pdf",
            "image/*",
            "text/*",
            "audio/*"
        ))
    }

    private fun displayPreview(bitmap: Bitmap) {
        binding.cardPreview.visibility = View.VISIBLE
        binding.imagePreview.setImageBitmap(bitmap)
        binding.textFileName.text = "photo_${System.currentTimeMillis()}.png"
    }

    private fun displayFilePreview(uri: Uri) {
        binding.cardPreview.visibility = View.VISIBLE
        binding.textFileName.text = selectedFileName ?: "Unknown file"

        // Try to display image preview
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                binding.imagePreview.setImageBitmap(bitmap)
            } else {
                binding.imagePreview.setImageResource(com.verumdec.R.drawable.ic_document)
            }
        } catch (e: Exception) {
            binding.imagePreview.setImageResource(com.verumdec.R.drawable.ic_document)
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun saveEvidence() {
        val currentCaseId = caseId ?: return

        binding.btnSaveEvidence.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                processAndSaveEvidence(currentCaseId)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ScannerActivity,
                        "Evidence saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSaveEvidence.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ScannerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun processAndSaveEvidence(caseId: String) = withContext(Dispatchers.IO) {
        capturedBitmap?.let { bitmap ->
            val fileName = "photo_${System.currentTimeMillis()}.png"
            forensicEngine.addImage(caseId, bitmap, fileName)
            return@withContext
        }

        selectedUri?.let { uri ->
            val fileName = selectedFileName ?: "file_${System.currentTimeMillis()}"
            
            // Determine type and add accordingly
            when {
                fileName.endsWith(".png", true) ||
                fileName.endsWith(".jpg", true) ||
                fileName.endsWith(".jpeg", true) -> {
                    // Handle as image
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (bitmap != null) {
                        forensicEngine.addImage(caseId, bitmap, fileName)
                    } else {
                        forensicEngine.addDocument(caseId, uri, fileName)
                    }
                }
                fileName.endsWith(".mp3", true) ||
                fileName.endsWith(".wav", true) ||
                fileName.endsWith(".m4a", true) -> {
                    forensicEngine.addAudio(caseId, uri, fileName)
                }
                else -> {
                    forensicEngine.addDocument(caseId, uri, fileName)
                }
            }
        }
    }
}
