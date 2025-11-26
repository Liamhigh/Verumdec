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
import com.verumomnis.forensic.video.VideoForensics
import java.io.File

@Composable
fun VideoInputScreen(nav: NavController) {
    val context = LocalContext.current
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var resultFlags by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("No file selected") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        fileUri = uri
        selectedFilename = uri?.lastPathSegment ?: "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Video Evidence", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(20.dp))

        VerumButton(text = "Choose Video File") {
            pickVideo.launch("video/*")
        }

        Spacer(Modifier.height(20.dp))
        
        Text(
            "Selected: $selectedFilename",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        fileUri?.let { uri ->
            Spacer(Modifier.height(20.dp))

            VerumButton(
                text = if (isProcessing) "Processing..." else "Run Video Forensic Scan",
                enabled = !isProcessing
            ) {
                isProcessing = true
                val file = uriToFile(context, uri, "video")
                val forensic = VideoForensics.analyse(file)

                resultFlags = buildString {
                    appendLine("=== VIDEO FORENSIC ANALYSIS ===\n")
                    appendLine("Frame Integrity Score: ${String.format("%.2f", forensic.frameIntegrity)}")
                    appendLine("GOP Integrity Score: ${String.format("%.2f", forensic.gopIntegrity)}")
                    appendLine("Bitrate Drift Score: ${String.format("%.2f", forensic.bitrateScore)}")
                    appendLine("A/V Sync Drift: ${String.format("%.2f", forensic.avSyncScore)}")
                    appendLine("Motion Warp Score: ${String.format("%.2f", forensic.motionWarpScore)}")
                    appendLine("\nFlags:")
                    if (forensic.flags.isEmpty()) {
                        appendLine("  No issues detected.")
                    } else {
                        forensic.flags.forEach { appendLine("  • $it") }
                    }
                }

                // Create sealed PDF report
                val reportText = buildString {
                    appendLine("VERUM OMNIS VIDEO FORENSICS REPORT")
                    appendLine("=" .repeat(40))
                    appendLine("File: $selectedFilename")
                    appendLine()
                    appendLine(resultFlags)
                }
                val reportFile = File(context.filesDir, "video_forensics_${System.currentTimeMillis()}.txt")
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

private fun uriToFile(context: android.content.Context, uri: Uri, prefix: String): File {
    val input = context.contentResolver.openInputStream(uri)
    val file = File(context.cacheDir, "${prefix}_input_${System.currentTimeMillis()}.tmp")
    file.outputStream().use { output -> input?.copyTo(output) }
    return file
}
