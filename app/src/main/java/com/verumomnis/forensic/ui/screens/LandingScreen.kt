package com.verumomnis.forensic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.verumomnis.forensic.ui.components.VerumLogoHeader
import com.verumomnis.forensic.ui.components.VerumSectionCard

@Composable
fun LandingScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        VerumLogoHeader()

        Text(
            "Verum Omnis – Forensic AI",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Free for Private Citizens.\n" +
            "Institutions pay 20% of recovered fraud.\n" +
            "All analysis done offline on-device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(30.dp))

        VerumSectionCard("Begin Analysis") {
            nav.navigate("role")
        }
    }
}
