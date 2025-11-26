package com.verumomnis.forensic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.verumomnis.forensic.ui.components.VerumButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SealedReportsScreen(nav: NavController) {
    val context = LocalContext.current
    var reports by remember { mutableStateOf(listOf<File>()) }

    LaunchedEffect(Unit) {
        reports = context.filesDir.listFiles()
            ?.filter { it.name.startsWith("VO_Sealed_Report") && it.name.endsWith(".pdf") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Sealed Reports", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Your locally saved SHA-512 sealed forensic reports:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No sealed reports yet.\nRun an analysis to generate one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportCard(report)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        VerumButton(text = "Back to Dashboard") {
            nav.navigate("dashboard") {
                popUpTo("dashboard") { inclusive = true }
            }
        }
    }
}

@Composable
private fun ReportCard(file: File) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔐 ${file.name}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Created: ${dateFormat.format(Date(file.lastModified()))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Size: ${file.length() / 1024} KB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
