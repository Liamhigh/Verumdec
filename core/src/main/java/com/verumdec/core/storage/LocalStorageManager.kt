package com.verumdec.core.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.verumdec.core.util.FileUtils
import com.verumdec.core.util.HashUtils
import com.verumdec.core.util.SafeWriteUtils
import java.io.File
import java.util.*

/**
 * Local storage manager for cases and evidence.
 * All data is stored locally with no network access.
 */
class LocalStorageManager(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .setPrettyPrinting()
        .create()

    companion object {
        private const val CASE_FILE = "case.json"
        private const val EVIDENCE_INDEX = "evidence_index.json"
        private const val METADATA_FILE = "metadata.json"
    }

    /**
     * Save a case to local storage.
     */
    fun saveCase(case: StoredCase): Boolean {
        val caseDir = File(FileUtils.getCasesDir(context), case.id)
        caseDir.mkdirs()
        
        val caseFile = File(caseDir, CASE_FILE)
        val json = gson.toJson(case)
        
        return SafeWriteUtils.safeWriteText(caseFile, json)
    }

    /**
     * Load a case from local storage.
     */
    fun loadCase(caseId: String): StoredCase? {
        val caseFile = File(FileUtils.getCasesDir(context), "$caseId/$CASE_FILE")
        if (!caseFile.exists()) return null
        
        return try {
            val json = caseFile.readText()
            gson.fromJson(json, StoredCase::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete a case from local storage.
     */
    fun deleteCase(caseId: String): Boolean {
        val caseDir = File(FileUtils.getCasesDir(context), caseId)
        return SafeWriteUtils.safeDelete(caseDir)
    }

    /**
     * List all stored cases.
     */
    fun listCases(): List<CaseSummary> {
        val casesDir = FileUtils.getCasesDir(context)
        val summaries = mutableListOf<CaseSummary>()
        
        casesDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val caseFile = File(dir, CASE_FILE)
            if (caseFile.exists()) {
                try {
                    val json = caseFile.readText()
                    val case = gson.fromJson(json, StoredCase::class.java)
                    summaries.add(CaseSummary(
                        id = case.id,
                        name = case.name,
                        createdAt = case.createdAt,
                        updatedAt = case.updatedAt,
                        evidenceCount = case.evidenceIds.size,
                        status = case.status
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        return summaries.sortedByDescending { it.updatedAt }
    }

    /**
     * Save evidence file.
     */
    fun saveEvidence(caseId: String, evidence: StoredEvidence, content: ByteArray): Boolean {
        val evidenceDir = FileUtils.getEvidenceDir(context, caseId)
        
        // Save the actual file
        val evidenceFile = File(evidenceDir, "${evidence.id}_${evidence.fileName}")
        if (!SafeWriteUtils.safeWriteBytes(evidenceFile, content)) {
            return false
        }
        
        // Save evidence metadata
        val metadataFile = File(evidenceDir, "${evidence.id}.meta.json")
        val metadataJson = gson.toJson(evidence)
        
        return SafeWriteUtils.safeWriteText(metadataFile, metadataJson)
    }

    /**
     * Load evidence metadata.
     */
    fun loadEvidenceMetadata(caseId: String, evidenceId: String): StoredEvidence? {
        val evidenceDir = FileUtils.getEvidenceDir(context, caseId)
        val metadataFile = File(evidenceDir, "$evidenceId.meta.json")
        
        if (!metadataFile.exists()) return null
        
        return try {
            val json = metadataFile.readText()
            gson.fromJson(json, StoredEvidence::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load evidence file content.
     */
    fun loadEvidenceContent(caseId: String, evidenceId: String): ByteArray? {
        val evidenceDir = FileUtils.getEvidenceDir(context, caseId)
        val evidence = loadEvidenceMetadata(caseId, evidenceId) ?: return null
        
        val evidenceFile = File(evidenceDir, "${evidenceId}_${evidence.fileName}")
        
        return if (evidenceFile.exists()) evidenceFile.readBytes() else null
    }

    /**
     * Delete evidence.
     */
    fun deleteEvidence(caseId: String, evidenceId: String): Boolean {
        val evidenceDir = FileUtils.getEvidenceDir(context, caseId)
        val evidence = loadEvidenceMetadata(caseId, evidenceId) ?: return false
        
        val evidenceFile = File(evidenceDir, "${evidenceId}_${evidence.fileName}")
        val metadataFile = File(evidenceDir, "$evidenceId.meta.json")
        
        return SafeWriteUtils.safeDelete(evidenceFile) && SafeWriteUtils.safeDelete(metadataFile)
    }

    /**
     * Save generated report.
     */
    fun saveReport(report: StoredReport, pdfContent: ByteArray): File? {
        val reportsDir = FileUtils.getReportsDir(context)
        val reportFile = File(reportsDir, report.fileName)
        
        return if (SafeWriteUtils.safeWriteBytes(reportFile, pdfContent)) {
            // Save report metadata
            val metadataFile = File(reportsDir, "${report.id}.meta.json")
            SafeWriteUtils.safeWriteText(metadataFile, gson.toJson(report))
            reportFile
        } else {
            null
        }
    }

    /**
     * List generated reports.
     */
    fun listReports(): List<StoredReport> {
        val reportsDir = FileUtils.getReportsDir(context)
        val reports = mutableListOf<StoredReport>()
        
        reportsDir.listFiles()?.filter { it.name.endsWith(".meta.json") }?.forEach { metaFile ->
            try {
                val json = metaFile.readText()
                reports.add(gson.fromJson(json, StoredReport::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return reports.sortedByDescending { it.generatedAt }
    }

    /**
     * Get storage statistics.
     */
    fun getStorageStats(): StorageStats {
        val casesDir = FileUtils.getCasesDir(context)
        val reportsDir = FileUtils.getReportsDir(context)
        
        var totalCaseSize = 0L
        var evidenceCount = 0
        
        casesDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                totalCaseSize += file.length()
                if (file.name.endsWith(".meta.json") && !file.name.contains("case")) {
                    evidenceCount++
                }
            }
        }
        
        var reportSize = 0L
        var reportCount = 0
        reportsDir.listFiles()?.filter { it.name.endsWith(".pdf") }?.forEach { file ->
            reportSize += file.length()
            reportCount++
        }
        
        return StorageStats(
            caseCount = listCases().size,
            evidenceCount = evidenceCount,
            reportCount = reportCount,
            totalCaseStorageBytes = totalCaseSize,
            totalReportStorageBytes = reportSize,
            totalStorageBytes = totalCaseSize + reportSize
        )
    }

    /**
     * Clear all temporary files.
     */
    fun clearTempFiles() {
        val tempDir = FileUtils.getTempDir(context)
        SafeWriteUtils.safeDelete(tempDir)
        tempDir.mkdirs()
    }
}

// Data classes for storage

data class StoredCase(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val status: CaseStatus = CaseStatus.IN_PROGRESS,
    val evidenceIds: MutableList<String> = mutableListOf(),
    val entityIds: MutableList<String> = mutableListOf(),
    val analysisCompleted: Boolean = false,
    val reportGenerated: Boolean = false,
    val reportId: String? = null,
    val notes: String = ""
)

enum class CaseStatus {
    NEW, IN_PROGRESS, ANALYSIS_COMPLETE, REPORT_GENERATED, ARCHIVED
}

data class CaseSummary(
    val id: String,
    val name: String,
    val createdAt: Date,
    val updatedAt: Date,
    val evidenceCount: Int,
    val status: CaseStatus
)

data class StoredEvidence(
    val id: String = UUID.randomUUID().toString(),
    val caseId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val sha512Hash: String,
    val addedAt: Date = Date(),
    val processedAt: Date? = null,
    val evidenceType: String,
    val extractedText: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class StoredReport(
    val id: String = UUID.randomUUID().toString(),
    val caseId: String,
    val caseName: String,
    val fileName: String,
    val sha512Hash: String,
    val generatedAt: Date = Date(),
    val pageCount: Int,
    val entityCount: Int,
    val contradictionCount: Int
)

data class StorageStats(
    val caseCount: Int,
    val evidenceCount: Int,
    val reportCount: Int,
    val totalCaseStorageBytes: Long,
    val totalReportStorageBytes: Long,
    val totalStorageBytes: Long
)
