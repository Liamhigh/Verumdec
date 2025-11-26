package com.verumomnis.forensic.ui.screens

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
import com.verumomnis.forensic.ui.components.VerumButton
import com.verumomnis.forensic.voice.VoiceForensics
import java.io.File

@Composable
fun AudioInputScreen(nav: NavController) {
    val context = LocalContext.current
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var resultFlags by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("No audio selected") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        audioUri = uri
        selectedFilename = uri?.lastPathSegment ?: "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Audio / Voice Forensics", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(20.dp))

        VerumButton(text = "Choose Audio File") {
            pickAudio.launch("audio/*")
        }

        Spacer(Modifier.height(20.dp))
        
        Text(
            "Selected: $selectedFilename",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        audioUri?.let { uri ->
            Spacer(Modifier.height(20.dp))

            VerumButton(
                text = if (isProcessing) "Processing..." else "Run Voice Forensic Scan",
                enabled = !isProcessing
            ) {
                isProcessing = true
                
                val input = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "audio_input_${System.currentTimeMillis()}.tmp")
                file.outputStream().use { output -> input?.copyTo(output) }
                
                val forensic = VoiceForensics.analyse(file)
                
                resultFlags = buildString {
                    appendLine("=== VOICE FORENSIC ANALYSIS ===\n")
                    appendLine("MFCC Score: ${String.format("%.2f", forensic.mfccScore)}")
                    appendLine("Spectral Score: ${String.format("%.2f", forensic.spectralScore)}")
                    appendLine("Envelope Score: ${String.format("%.2f", forensic.envelopeScore)}")
                    appendLine()
                    appendLine("Flags:")
                    if (forensic.flags.isEmpty()) {
                        appendLine("  No deepfake indicators detected ✓")
                    } else {
                        forensic.flags.forEach { appendLine("  ⚠️ $it") }
                    }
                }

                // Create sealed PDF report
                val reportText = buildString {
                    appendLine("VERUM OMNIS VOICE FORENSICS REPORT")
                    appendLine("=" .repeat(40))
                    appendLine("File: $selectedFilename")
                    appendLine()
                    appendLine(resultFlags)
                }
                val reportFile = File(context.filesDir, "voice_forensics_${System.currentTimeMillis()}.txt")
                reportFile.writeText(reportText)
                VerumOmnisEngine.processPdf(context, reportFile)
                
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
