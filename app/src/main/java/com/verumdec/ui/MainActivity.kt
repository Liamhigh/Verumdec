package com.verumdec.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityMainBinding
import com.verumdec.core.leveler.Leveler
import com.verumdec.core.leveler.LevelerOutput
import com.verumdec.core.leveler.CaseFile
import com.verumdec.core.leveler.DocumentInfo
import com.verumdec.core.leveler.DocumentType
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), Leveler.ProgressListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: Leveler
    private lateinit var evidenceAdapter: EvidenceAdapter
    
    private var currentCase: Case? = null
    private var currentCaseFile: CaseFile? = null
    private var lastLevelerOutput: LevelerOutput? = null
    private val evidenceUris = mutableMapOf<String, Uri>()

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addEvidence(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NEW: Initialize Leveler engine instead of old ContradictionEngine
        engine = Leveler(this)
        setupUI()
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)

        // Setup evidence RecyclerView
        evidenceAdapter = EvidenceAdapter(
            onDeleteClick = { evidence -> removeEvidence(evidence) }
        )
        binding.recyclerEvidence.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = evidenceAdapter
        }

        // Setup buttons
        binding.btnNewCase.setOnClickListener { showNewCaseDialog() }
        binding.btnOpenCase.setOnClickListener { 
            Toast.makeText(this, "Open case feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnAddEvidence.setOnClickListener { pickFile() }
        binding.fabAdd.setOnClickListener { pickFile() }
        binding.btnAnalyze.setOnClickListener { runAnalysis() }
        binding.btnViewResults.setOnClickListener { viewResults() }

        updateUI()
    }

    private fun showNewCaseDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Enter case name"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("New Case")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text?.toString()?.trim()
                if (!name.isNullOrEmpty()) {
                    createNewCase(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewCase(name: String) {
        currentCase = Case(name = name)
        currentCaseFile = CaseFile(name = name)
        lastLevelerOutput = null
        evidenceUris.clear()
        evidenceAdapter.submitList(emptyList())
        binding.cardStats.visibility = View.GONE
        updateUI()
        Toast.makeText(this, "Case '$name' created", Toast.LENGTH_SHORT).show()
    }

    private fun pickFile() {
        if (currentCase == null) {
            showNewCaseDialog()
            return
        }

        pickFileLauncher.launch(arrayOf(
            "application/pdf",
            "image/*",
            "text/*"
        ))
    }

    private fun addEvidence(uri: Uri) {
        val case = currentCase ?: return
        val caseFile = currentCaseFile ?: return

        // Get file name
        val cursor = contentResolver.query(uri, null, null, null, null)
        var fileName = "Unknown"
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }

        // Persist permission
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        // Create Evidence for old data model (for UI compatibility)
        val evidenceType = getEvidenceType(fileName)
        val evidence = Evidence(
            type = evidenceType,
            fileName = fileName,
            filePath = uri.toString()
        )

        case.evidence.add(evidence)
        evidenceUris[evidence.id] = uri
        
        // Also add to new CaseFile for Leveler
        val documentType = mapEvidenceTypeToDocumentType(evidenceType)
        val documentInfo = DocumentInfo(
            id = evidence.id,
            fileName = fileName,
            type = documentType
        )
        caseFile.documents.add(documentInfo)
        
        evidenceAdapter.submitList(case.evidence.toList())
        updateUI()
    }

    private fun removeEvidence(evidence: Evidence) {
        currentCase?.evidence?.remove(evidence)
        currentCaseFile?.documents?.removeAll { it.id == evidence.id }
        evidenceUris.remove(evidence.id)
        evidenceAdapter.submitList(currentCase?.evidence?.toList() ?: emptyList())
        updateUI()
    }

    private fun runAnalysis() {
        val case = currentCase ?: return
        val caseFile = currentCaseFile ?: return
        if (case.evidence.isEmpty()) {
            Toast.makeText(this, "Add some evidence first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress dialog
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Analyzing with Leveler Engine...")
            .setMessage("Processing documents...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                // Run the NEW Leveler engine
                val output = engine.run(caseFile, evidenceUris, this@MainActivity)
                lastLevelerOutput = output
                
                // Convert Leveler output to old Case format for UI compatibility
                currentCase = convertLevelerOutputToCase(case, output)
                
                progressDialog.dismiss()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Convert LevelerOutput to the old Case format for UI compatibility.
     */
    private fun convertLevelerOutputToCase(originalCase: Case, output: LevelerOutput): Case {
        val entities = output.speakerMap.speakers.values.map { profile ->
            Entity(
                id = profile.id,
                primaryName = profile.name,
                aliases = profile.aliases,
                mentions = profile.statementCount
            )
        }.toMutableList()

        val timeline = output.normalizedTimeline.events.map { event ->
            TimelineEvent(
                date = Date(event.timestamp),
                description = event.description,
                sourceEvidenceId = event.documentId,
                entityIds = listOf(event.speaker),
                eventType = mapEventType(event.eventType),
                significance = Significance.NORMAL
            )
        }.toMutableList()

        val contradictions = output.contradictionSet.contradictions.map { entry ->
            val sourceStatement = Statement(
                entityId = entry.sourceSpeaker,
                text = entry.sourceText,
                sourceEvidenceId = entry.sourceDocument,
                type = StatementType.CLAIM
            )
            val targetStatement = Statement(
                entityId = entry.targetSpeaker,
                text = entry.targetText,
                sourceEvidenceId = entry.targetDocument,
                type = StatementType.DENIAL
            )
            Contradiction(
                entityId = entry.sourceSpeaker,
                statementA = sourceStatement,
                statementB = targetStatement,
                type = mapContradictionType(entry.type),
                severity = mapSeverity(entry.severity),
                description = entry.description,
                legalImplication = entry.legalTrigger
            )
        }.toMutableList()

        val liabilityScores = output.liabilityScores.mapValues { (_, score) ->
            LiabilityScore(
                entityId = score.entityId,
                overallScore = score.overallScore,
                contradictionScore = score.contradictionScore,
                behavioralScore = score.behavioralScore,
                evidenceContributionScore = score.evidenceContributionScore,
                chronologicalConsistencyScore = score.consistencyScore,
                causalResponsibilityScore = 0f,
                breakdown = LiabilityBreakdown(
                    totalContradictions = score.contradictionCount,
                    criticalContradictions = score.breakdown.criticalContradictions,
                    behavioralFlags = score.breakdown.behavioralFlags,
                    storyChanges = score.breakdown.storyChanges
                )
            )
        }.toMutableMap()

        val narrative = buildString {
            appendLine("=== LEVELER ENGINE ANALYSIS REPORT ===")
            appendLine("Engine Version: ${output.engineVersion}")
            appendLine()
            appendLine("Extraction Summary:")
            appendLine("- Documents processed: ${output.extractionSummary.processedDocuments}/${output.extractionSummary.totalDocuments}")
            appendLine("- Statements extracted: ${output.extractionSummary.totalStatements}")
            appendLine("- Speakers identified: ${output.extractionSummary.speakersIdentified}")
            appendLine("- Timeline events: ${output.extractionSummary.timelineEvents}")
            appendLine()
            appendLine("Contradiction Analysis:")
            appendLine("- Total contradictions: ${output.contradictionSet.totalCount}")
            appendLine("- Critical: ${output.contradictionSet.criticalCount}")
            appendLine("- High: ${output.contradictionSet.highCount}")
            appendLine("- Medium: ${output.contradictionSet.mediumCount}")
            appendLine("- Low: ${output.contradictionSet.lowCount}")
            appendLine()
            appendLine("Behavioral Analysis:")
            appendLine("- Total shifts detected: ${output.behaviorShiftReport.totalShifts}")
            appendLine("- Affected speakers: ${output.behaviorShiftReport.affectedSpeakers.size}")
            for ((pattern, count) in output.behaviorShiftReport.patternBreakdown) {
                appendLine("  - $pattern: $count")
            }
        }

        return originalCase.copy(
            entities = entities,
            timeline = timeline,
            contradictions = contradictions,
            liabilityScores = liabilityScores,
            narrative = narrative
        )
    }

    private fun viewResults() {
        val case = currentCase ?: return
        
        val intent = Intent(this, AnalysisActivity::class.java).apply {
            putExtra("case_id", case.id)
        }
        // Store case and output for the analysis activity
        AnalysisActivity.currentCase = case
        AnalysisActivity.levelerOutput = lastLevelerOutput
        startActivity(intent)
    }

    private fun updateUI() {
        val case = currentCase
        
        binding.textCaseName.text = case?.name ?: "No case loaded"
        
        val evidenceCount = case?.evidence?.size ?: 0
        binding.textEvidenceCount.text = "$evidenceCount files"
        binding.textNoEvidence.visibility = if (evidenceCount == 0) View.VISIBLE else View.GONE
        binding.recyclerEvidence.visibility = if (evidenceCount > 0) View.VISIBLE else View.GONE
        
        binding.btnAnalyze.isEnabled = evidenceCount > 0
        
        // Show stats if analysis has been run
        if (case != null && case.entities.isNotEmpty()) {
            binding.cardStats.visibility = View.VISIBLE
            binding.textEntitiesCount.text = case.entities.size.toString()
            binding.textContradictionsCount.text = case.contradictions.size.toString()
            binding.textTimelineCount.text = case.timeline.size.toString()
        }
    }

    // Leveler.ProgressListener implementation
    override fun onProgressUpdate(
        stage: Leveler.AnalysisStage,
        progress: Int,
        message: String
    ) {
        runOnUiThread {
            // Update progress if needed - could update a progress dialog here
        }
    }

    override fun onComplete(output: LevelerOutput) {
        runOnUiThread {
            lastLevelerOutput = output
            // The case is already updated in runAnalysis, but we can update UI here
            updateUI()
            Toast.makeText(this, "Leveler analysis complete!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Leveler Error: $error", Toast.LENGTH_LONG).show()
        }
    }

    // Helper functions to map between old and new types
    private fun getEvidenceType(fileName: String): EvidenceType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> EvidenceType.PDF
            "jpg", "jpeg", "png", "gif", "bmp" -> EvidenceType.IMAGE
            "txt", "doc", "docx" -> EvidenceType.TEXT
            else -> EvidenceType.UNKNOWN
        }
    }

    private fun mapEvidenceTypeToDocumentType(evidenceType: EvidenceType): DocumentType {
        return when (evidenceType) {
            EvidenceType.PDF -> DocumentType.PDF
            EvidenceType.IMAGE -> DocumentType.IMAGE
            EvidenceType.TEXT -> DocumentType.TEXT
            EvidenceType.EMAIL -> DocumentType.EMAIL
            EvidenceType.WHATSAPP -> DocumentType.CHAT
            EvidenceType.UNKNOWN -> DocumentType.UNKNOWN
        }
    }

    private fun mapEventType(eventType: String): EventType {
        return when (eventType) {
            "PAYMENT" -> EventType.PAYMENT
            "PROMISE" -> EventType.PROMISE
            "AGREEMENT" -> EventType.DOCUMENT
            "STATEMENT" -> EventType.COMMUNICATION
            else -> EventType.OTHER
        }
    }

    private fun mapContradictionType(type: com.verumdec.core.model.ContradictionType): ContradictionType {
        return when (type) {
            com.verumdec.core.model.ContradictionType.DIRECT -> ContradictionType.DIRECT
            com.verumdec.core.model.ContradictionType.CROSS_DOCUMENT -> ContradictionType.CROSS_DOCUMENT
            com.verumdec.core.model.ContradictionType.TIMELINE -> ContradictionType.TEMPORAL
            com.verumdec.core.model.ContradictionType.ENTITY -> ContradictionType.DIRECT
            com.verumdec.core.model.ContradictionType.BEHAVIORAL -> ContradictionType.BEHAVIORAL
            com.verumdec.core.model.ContradictionType.SEMANTIC -> ContradictionType.DIRECT
            com.verumdec.core.model.ContradictionType.FINANCIAL -> ContradictionType.DIRECT
            com.verumdec.core.model.ContradictionType.MISSING_EVIDENCE -> ContradictionType.MISSING_EVIDENCE
        }
    }

    private fun mapSeverity(severity: Int): Severity {
        return when {
            severity >= 9 -> Severity.CRITICAL
            severity >= 7 -> Severity.HIGH
            severity >= 5 -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }
}
