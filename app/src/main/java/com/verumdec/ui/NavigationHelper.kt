package com.verumdec.ui

import android.content.Context
import android.content.Intent
import com.verumdec.data.Case
import java.io.File

/**
 * Navigation helper for creating intents and navigating between activities.
 */
object NavigationHelper {

    /**
     * Navigate to VerifyDocumentActivity.
     */
    fun navigateToVerifyDocument(context: Context) {
        context.startActivity(VerifyDocumentActivity.newIntent(context))
    }

    /**
     * Navigate to EvidenceTimelineActivity with case data.
     */
    fun navigateToTimeline(context: Context, case: Case) {
        context.startActivity(EvidenceTimelineActivity.newIntent(context, case))
    }

    /**
     * Navigate to ForensicReportViewerActivity with PDF file.
     */
    fun navigateToReportViewer(context: Context, pdfFile: File) {
        context.startActivity(ForensicReportViewerActivity.newIntent(context, pdfFile))
    }

    /**
     * Navigate to AnalysisActivity with case data.
     */
    fun navigateToAnalysis(context: Context, case: Case) {
        val intent = Intent(context, AnalysisActivity::class.java).apply {
            putExtra("case_id", case.id)
        }
        AnalysisActivity.currentCase = case
        context.startActivity(intent)
    }

    /**
     * Create intent for sharing a PDF report.
     */
    fun createShareReportIntent(context: Context, pdfFile: File, authority: String): Intent {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            authority,
            pdfFile
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Show ConstitutionAlertDialog for critical contradictions.
     */
    fun showConstitutionAlert(
        context: Context,
        fragmentManager: androidx.fragment.app.FragmentManager,
        case: Case,
        onAcknowledge: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        if (case.contradictions.isNotEmpty()) {
            ConstitutionAlertDialog.builder(context)
                .setContradictions(case.contradictions)
                .setOnAcknowledgeListener(onAcknowledge)
                .setOnDismissListener(onDismiss)
                .show(fragmentManager)
        }
    }
}
