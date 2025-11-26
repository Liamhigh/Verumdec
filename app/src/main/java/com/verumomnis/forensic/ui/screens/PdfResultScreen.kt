package com.verumomnis.forensic.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.verumomnis.forensic.ui.components.VerumButton
import java.io.File

@Composable
fun PdfResultScreen(nav: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Report Generated",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "🔐",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "SHA-512 Sealed PDF Created",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Your forensic report has been sealed with a SHA-512 hash and saved locally.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        VerumButton(text = "Share Report") {
            // Get the latest report file
            val reportsDir = context.filesDir
            val latestReport = reportsDir.listFiles()
                ?.filter { it.name.startsWith("VO_Sealed_Report") }
                ?.maxByOrNull { it.lastModified() }

            latestReport?.let { file ->
                shareFile(context, file)
            }
        }

        Spacer(Modifier.height(12.dp))

        VerumButton(text = "Back to Dashboard") {
            nav.navigate("dashboard") {
                popUpTo("dashboard") { inclusive = true }
            }
        }
    }
}

private fun shareFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Forensic Report"))
    } catch (e: Exception) {
        // Handle error silently
    }
}
