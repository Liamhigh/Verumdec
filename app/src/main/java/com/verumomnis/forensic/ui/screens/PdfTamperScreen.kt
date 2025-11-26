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
import com.verumomnis.forensic.pdfscan.PdfTamperDetector
import com.verumomnis.forensic.ui.components.VerumButton
import java.io.File

@Composable
fun PdfTamperScreen(nav: NavController) {
    val context = LocalContext.current
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var resultFlags by remember { mutableStateOf("") }
    var selectedFilename by remember { mutableStateOf("No PDF selected") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        pdfUri = uri
        selectedFilename = uri?.lastPathSegment ?: "Unknown"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("PDF Tamper Detection", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(20.dp))

        VerumButton(text = "Choose PDF File") {
            pickPdf.launch("application/pdf")
        }

        Spacer(Modifier.height(20.dp))
        
        Text(
            "Selected: $selectedFilename",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        pdfUri?.let { uri ->
            Spacer(Modifier.height(20.dp))

            VerumButton(
                text = if (isProcessing) "Processing..." else "Run Tamper Detection",
                enabled = !isProcessing
            ) {
                isProcessing = true
                
                val input = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "pdf_input_${System.currentTimeMillis()}.pdf")
                file.outputStream().use { output -> input?.copyTo(output) }
                
                val tamper = PdfTamperDetector.analyse(context, file)
                
                resultFlags = buildString {
                    appendLine("=== PDF TAMPER ANALYSIS ===\n")
                    appendLine("Incremental Updates: ${if (tamper.hasIncrementalUpdates) "YES ⚠️" else "No"}")
                    appendLine()
                    
                    appendLine("XRef Issues:")
                    if (tamper.xrefIssues.isEmpty()) {
                        appendLine("  None detected")
                    } else {
                        tamper.xrefIssues.forEach { appendLine("  • $it") }
                    }
                    appendLine()
                    
                    appendLine("Metadata Issues:")
                    if (tamper.metadataIssues.isEmpty()) {
                        appendLine("  None detected")
                    } else {
                        tamper.metadataIssues.forEach { appendLine("  • $it") }
                    }
                    appendLine()
                    
                    appendLine("Object Issues:")
                    if (tamper.objectIssues.isEmpty()) {
                        appendLine("  None detected")
                    } else {
                        tamper.objectIssues.forEach { appendLine("  • $it") }
                    }
                    appendLine()
                    
                    appendLine("Embedded Objects:")
                    if (tamper.embeddedObjects.isEmpty()) {
                        appendLine("  None")
                    } else {
                        tamper.embeddedObjects.forEach { appendLine("  • $it") }
                    }
                    appendLine()
                    
                    appendLine("Overall Flags:")
                    if (tamper.flags.isEmpty()) {
                        appendLine("  No issues detected ✓")
                    } else {
                        tamper.flags.forEach { appendLine("  ⚠️ $it") }
                    }
                }

                // Create sealed PDF report
                val reportText = buildString {
                    appendLine("VERUM OMNIS PDF TAMPER REPORT")
                    appendLine("=" .repeat(40))
                    appendLine("File: $selectedFilename")
                    appendLine()
                    appendLine(resultFlags)
                }
                val reportFile = File(context.filesDir, "pdf_tamper_report_${System.currentTimeMillis()}.txt")
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
