package com.verumomnis.forensic.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.verumomnis.forensic.VerumOmnisEngine
import com.verumomnis.forensic.image.ImageForgeryDetector
import com.verumomnis.forensic.ui.components.VerumButton
import java.io.File

@Composable
fun ImageInputScreen(nav: NavController) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var resultFlags by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("No image selected") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
        selectedFilename = uri?.lastPathSegment ?: "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Image Forensics", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(20.dp))

        VerumButton(text = "Choose Image") {
            pickImage.launch("image/*")
        }

        Spacer(Modifier.height(20.dp))
        
        Text(
            "Selected: $selectedFilename",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        imageUri?.let { uri ->
            Spacer(Modifier.height(20.dp))

            VerumButton(
                text = if (isProcessing) "Processing..." else "Run Image Forensic Scan",
                enabled = !isProcessing
            ) {
                isProcessing = true
                
                val input = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                
                if (bitmap != null) {
                    val forensic = ImageForgeryDetector.analyse(bitmap, emptyMap())
                    
                    resultFlags = buildString {
                        appendLine("=== IMAGE FORENSIC ANALYSIS ===\n")
                        appendLine("ELA Score: ${String.format("%.2f", forensic.elaScore)}")
                        appendLine("Noise Score: ${String.format("%.2f", forensic.noiseScore)}")
                        appendLine()
                        appendLine("EXIF Anomalies:")
                        if (forensic.exifAnomalies.isEmpty()) {
                            appendLine("  None detected")
                        } else {
                            forensic.exifAnomalies.forEach { appendLine("  • $it") }
                        }
                        appendLine()
                        appendLine("Flags:")
                        if (forensic.flags.isEmpty()) {
                            appendLine("  No issues detected ✓")
                        } else {
                            forensic.flags.forEach { appendLine("  ⚠️ $it") }
                        }
                    }

                    // Create sealed PDF report
                    val reportText = buildString {
                        appendLine("VERUM OMNIS IMAGE FORENSICS REPORT")
                        appendLine("=" .repeat(40))
                        appendLine("File: $selectedFilename")
                        appendLine()
                        appendLine(resultFlags)
                    }
                    val reportFile = File(context.filesDir, "image_forensics_${System.currentTimeMillis()}.txt")
                    reportFile.writeText(reportText)
                    VerumOmnisEngine.processPdf(context, reportFile)
                } else {
                    resultFlags = "Error: Could not decode image."
                }
                
                isProcessing = false
            }

            if (resultFlags.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        resultFlags,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                
                VerumButton(text = "View Sealed Report") {
                    nav.navigate("pdf")
                }
            }
        }
    }
}
