package com.verumomnis.forensic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.verumomnis.forensic.VerumOmnisEngine
import com.verumomnis.forensic.ui.components.VerumButton

@Composable
fun ForensicScreen(nav: NavController) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Text Analysis",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Enter a statement, transcript, or message to analyze:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text("Paste or type text here...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(Modifier.height(20.dp))

        VerumButton(
            text = if (isProcessing) "Processing..." else "Run Contradiction Analysis",
            enabled = inputText.isNotBlank() && !isProcessing
        ) {
            isProcessing = true
            val result = VerumOmnisEngine.process(context, inputText)
            
            resultText = buildString {
                appendLine("=== ANALYSIS COMPLETE ===\n")
                
                appendLine("CONTRADICTIONS FOUND: ${result.contradictions.size}")
                if (result.contradictions.isEmpty()) {
                    appendLine("  No contradictions detected.\n")
                } else {
                    result.contradictions.forEach {
                        appendLine("  • $it")
                    }
                    appendLine()
                }
                
                appendLine("BEHAVIOURAL FLAGS: ${result.behaviouralFlags.size}")
                if (result.behaviouralFlags.isEmpty()) {
                    appendLine("  No behavioural red flags.\n")
                } else {
                    result.behaviouralFlags.forEach {
                        appendLine("  • $it")
                    }
                    appendLine()
                }
                
                appendLine("PDF SEALED: ${result.pdfFile.name}")
                appendLine("Location: ${result.pdfFile.absolutePath}")
            }
            
            isProcessing = false
        }

        Spacer(Modifier.height(8.dp))

        VerumButton(
            text = "Clear History",
            enabled = !isProcessing
        ) {
            VerumOmnisEngine.clearHistory()
            resultText = "Claim history cleared. Ready for new case analysis."
        }

        if (resultText.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Analysis Results",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        resultText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Claims analyzed: ${VerumOmnisEngine.getClaimCount()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
