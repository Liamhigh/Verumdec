package com.verumomnis.forensic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.verumomnis.forensic.ui.components.VerumSectionCard

@Composable
fun RoleSelectScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Select Your Role",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Choose how you want to use Verum Omnis:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(30.dp))

        VerumSectionCard("🧑 Private Citizen (Free)") {
            nav.navigate("dashboard")
        }

        VerumSectionCard("🏛 Institution (Licensed)") {
            nav.navigate("dashboard")
        }

        VerumSectionCard("⚖️ Legal Professional") {
            nav.navigate("dashboard")
        }
    }
}
