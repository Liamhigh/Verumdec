package com.verumomnis.forensic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class EvidenceTile(
    val title: String,
    val description: String,
    val emoji: String,
    val route: String
)

@Composable
fun EvidenceDashboardScreen(navController: NavController) {
    val tiles = listOf(
        EvidenceTile(
            title = "Text / Contradiction Engine",
            description = "Upload statements or transcripts and run Verum Omnis contradiction + SHA-512 sealed report.",
            emoji = "📝",
            route = "text_input"
        ),
        EvidenceTile(
            title = "Image Forensics",
            description = "Run ELA, metadata checks and tamper detection on photos and screenshots.",
            emoji = "🖼️",
            route = "image_input"
        ),
        EvidenceTile(
            title = "Voice / Audio Forensics",
            description = "Analyse recordings for threats, deepfake indicators and transcript contradictions.",
            emoji = "🎙️",
            route = "audio_input"
        ),
        EvidenceTile(
            title = "Video Forensics",
            description = "Run frame-hash, GOP, bitrate drift, motion warp and A/V sync checks on video files.",
            emoji = "🎥",
            route = "video_input"
        ),
        EvidenceTile(
            title = "PDF Tamper Detection",
            description = "Scan XRef, metadata, embedded objects and incremental updates for PDF tampering.",
            emoji = "📄",
            route = "pdf_tamper"
        ),
        EvidenceTile(
            title = "Sealed Reports",
            description = "Browse locally saved Verum Omnis SHA-512 sealed PDF reports.",
            emoji = "🔐",
            route = "sealed_reports"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Verum Omnis Evidence Hub",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Choose what kind of evidence you want to analyse offline. Every path ends in a SHA-512 sealed PDF.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tiles) { tile ->
                EvidenceTileCard(tile = tile) {
                    navController.navigate(tile.route)
                }
            }
        }
    }
}

@Composable
private fun EvidenceTileCard(
    tile: EvidenceTile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tile.emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tile.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
