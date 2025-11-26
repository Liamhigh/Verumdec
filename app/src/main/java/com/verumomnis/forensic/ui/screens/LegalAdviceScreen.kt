package com.verumomnis.forensic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.verumomnis.forensic.ui.components.VerumButton

@Composable
fun LegalAdviceScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Legal Advice Module",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "⚠️ DISCLAIMER",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This module provides informational guidance only. " +
                    "It does not constitute legal advice and should not be relied upon as such. " +
                    "Always consult a qualified legal professional for legal matters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Based on your forensic analysis, Verum Omnis can provide:",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        listOf(
            "Criminal law risk assessment",
            "Civil liability analysis",
            "Breach of contract evaluation",
            "Fraud pattern classification",
            "Recovery strategy suggestions",
            "Letter of demand templates",
            "Litigation forecasts",
            "Evidentiary admissibility guidance"
        ).forEach { item ->
            Text(
                "• $item",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        VerumButton(text = "Generate Legal Summary") {
            nav.navigate("pdf")
        }

        Spacer(Modifier.height(12.dp))

        VerumButton(text = "Back") {
            nav.popBackStack()
        }
    }
}
