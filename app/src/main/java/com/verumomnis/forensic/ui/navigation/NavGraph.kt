package com.verumomnis.forensic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.verumomnis.forensic.ui.screens.*

@Composable
fun VerumNavGraph(nav: NavHostController) {
    NavHost(navController = nav, startDestination = "landing") {
        composable("landing") { LandingScreen(nav) }
        composable("role") { RoleSelectScreen(nav) }
        composable("dashboard") { EvidenceDashboardScreen(nav) }
        composable("text_input") { ForensicScreen(nav) }
        composable("image_input") { ImageInputScreen(nav) }
        composable("audio_input") { AudioInputScreen(nav) }
        composable("video_input") { VideoInputScreen(nav) }
        composable("pdf_tamper") { PdfTamperScreen(nav) }
        composable("pdf") { PdfResultScreen(nav) }
        composable("sealed_reports") { SealedReportsScreen(nav) }
        composable("legal") { LegalAdviceScreen(nav) }
        composable("tax") { TaxReturnScreen(nav) }
    }
}
