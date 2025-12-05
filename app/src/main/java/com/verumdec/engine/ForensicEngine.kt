package com.verumdec.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.gson.Gson
import com.verumdec.data.Case
import com.verumdec.data.Evidence
import com.verumdec.data.EvidenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Forensic Engine for case management and evidence storage.
 * Handles creating cases, adding evidence, hashing, and report generation.
 */
class ForensicEngine(private val context: Context) {
    
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    
    /**
     * Creates a new case with folder structure:
     * /cases/{caseId}/
     *   case.json
     *   evidence/
     *   reports/
     */
    suspend fun createCase(caseName: String): String = withContext(Dispatchers.IO) {
        val caseId = UUID.randomUUID().toString()
        val caseDir = File(context.filesDir, "cases/$caseId")
        
        // Create folder structure
        File(caseDir, "evidence").mkdirs()
        File(caseDir, "reports").mkdirs()
        
        // Create case.json
        val case = Case(
            id = caseId,
            name = caseName,
            createdAt = Date()
        )
        
        val caseFile = File(caseDir, "case.json")
        caseFile.writeText(gson.toJson(case))
        
        caseId
    }
    
    /**
     * Loads a case from storage.
     */
    suspend fun loadCase(caseId: String): Case? = withContext(Dispatchers.IO) {
        try {
            val caseFile = File(context.filesDir, "cases/$caseId/case.json")
            if (!caseFile.exists()) return@withContext null
            
            val json = caseFile.readText()
            gson.fromJson(json, Case::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Saves case data to storage.
     */
    suspend fun saveCase(case: Case) = withContext(Dispatchers.IO) {
        val caseFile = File(context.filesDir, "cases/${case.id}/case.json")
        caseFile.writeText(gson.toJson(case))
    }
    
    /**
     * Adds a text note to the case.
     */
    suspend fun addTextNote(caseId: String, text: String): Evidence = withContext(Dispatchers.IO) {
        val evidenceId = UUID.randomUUID().toString()
        val fileName = "note_${System.currentTimeMillis()}.txt"
        val evidenceDir = File(context.filesDir, "cases/$caseId/evidence")
        evidenceDir.mkdirs()
        
        val noteFile = File(evidenceDir, fileName)
        noteFile.writeText(text)
        
        val evidence = Evidence(
            id = evidenceId,
            type = EvidenceType.TEXT,
            fileName = fileName,
            filePath = noteFile.absolutePath,
            extractedText = text,
            processed = true
        )
        
        // Update case
        val case = loadCase(caseId)
        case?.let {
            it.evidence.add(evidence)
            saveCase(it)
        }
        
        evidence
    }
    
    /**
     * Adds an image to the case evidence folder.
     */
    suspend fun addImage(caseId: String, bitmap: Bitmap, fileName: String): Evidence = withContext(Dispatchers.IO) {
        val evidenceId = UUID.randomUUID().toString()
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val evidenceDir = File(context.filesDir, "cases/$caseId/evidence")
        evidenceDir.mkdirs()
        
        val imageFile = File(evidenceDir, safeFileName)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        val evidence = Evidence(
            id = evidenceId,
            type = EvidenceType.IMAGE,
            fileName = safeFileName,
            filePath = imageFile.absolutePath,
            processed = false
        )
        
        // Update case
        val case = loadCase(caseId)
        case?.let {
            it.evidence.add(evidence)
            saveCase(it)
        }
        
        evidence
    }
    
    /**
     * Adds an audio file to the case evidence folder.
     */
    suspend fun addAudio(caseId: String, audioUri: Uri, fileName: String): Evidence = withContext(Dispatchers.IO) {
        val evidenceId = UUID.randomUUID().toString()
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val evidenceDir = File(context.filesDir, "cases/$caseId/evidence")
        evidenceDir.mkdirs()
        
        val audioFile = File(evidenceDir, safeFileName)
        context.contentResolver.openInputStream(audioUri)?.use { input ->
            FileOutputStream(audioFile).use { output ->
                input.copyTo(output)
            }
        }
        
        val evidence = Evidence(
            id = evidenceId,
            type = EvidenceType.UNKNOWN,
            fileName = safeFileName,
            filePath = audioFile.absolutePath,
            processed = false
        )
        
        // Update case
        val case = loadCase(caseId)
        case?.let {
            it.evidence.add(evidence)
            saveCase(it)
        }
        
        evidence
    }
    
    /**
     * Adds a document to the case evidence folder.
     */
    suspend fun addDocument(caseId: String, documentUri: Uri, fileName: String): Evidence = withContext(Dispatchers.IO) {
        val evidenceId = UUID.randomUUID().toString()
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val evidenceDir = File(context.filesDir, "cases/$caseId/evidence")
        evidenceDir.mkdirs()
        
        val docFile = File(evidenceDir, safeFileName)
        context.contentResolver.openInputStream(documentUri)?.use { input ->
            FileOutputStream(docFile).use { output ->
                input.copyTo(output)
            }
        }
        
        val evidence = Evidence(
            id = evidenceId,
            type = when {
                fileName.endsWith(".pdf", ignoreCase = true) -> EvidenceType.PDF
                else -> EvidenceType.UNKNOWN
            },
            fileName = safeFileName,
            filePath = docFile.absolutePath,
            processed = false
        )
        
        // Update case
        val case = loadCase(caseId)
        case?.let {
            it.evidence.add(evidence)
            saveCase(it)
        }
        
        evidence
    }
    
    /**
     * Calculates SHA-512 hash of all files in the case.
     */
    suspend fun hashCase(caseId: String): String = withContext(Dispatchers.IO) {
        val evidenceDir = File(context.filesDir, "cases/$caseId/evidence")
        val files = evidenceDir.listFiles()?.sortedBy { it.name } ?: emptyList()
        
        val digest = MessageDigest.getInstance("SHA-512")
        
        for (file in files) {
            if (file.isFile) {
                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            }
        }
        
        val hashBytes = digest.digest()
        hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Generates a text report for the case.
     */
    suspend fun generateReport(caseId: String): String = withContext(Dispatchers.IO) {
        val case = loadCase(caseId) ?: return@withContext ""
        
        val report = StringBuilder()
        report.appendLine("FORENSIC ANALYSIS REPORT")
        report.appendLine("=" .repeat(50))
        report.appendLine()
        report.appendLine("Case Name: ${case.name}")
        report.appendLine("Case ID: ${case.id}")
        report.appendLine("Created: ${dateFormat.format(case.createdAt)}")
        report.appendLine()
        
        report.appendLine("EVIDENCE")
        report.appendLine("-".repeat(50))
        for (evidence in case.evidence) {
            report.appendLine("- ${evidence.fileName} (${evidence.type})")
        }
        report.appendLine()
        
        report.appendLine("ENTITIES: ${case.entities.size}")
        for (entity in case.entities) {
            report.appendLine("- ${entity.primaryName}")
        }
        report.appendLine()
        
        report.appendLine("CONTRADICTIONS: ${case.contradictions.size}")
        for (contradiction in case.contradictions) {
            report.appendLine("- [${contradiction.severity}] ${contradiction.description}")
        }
        report.appendLine()
        
        report.appendLine("TIMELINE EVENTS: ${case.timeline.size}")
        report.appendLine()
        
        // Calculate and append hash
        val hash = hashCase(caseId)
        report.appendLine("SHA-512 SEAL")
        report.appendLine("-".repeat(50))
        report.appendLine(hash)
        report.appendLine()
        
        report.appendLine("Report generated: ${dateFormat.format(Date())}")
        
        // Save report to file
        val reportsDir = File(context.filesDir, "cases/$caseId/reports")
        reportsDir.mkdirs()
        val reportFile = File(reportsDir, "report.txt")
        reportFile.writeText(report.toString())
        
        report.toString()
    }
    
    /**
     * Lists all cases.
     */
    suspend fun listCases(): List<Case> = withContext(Dispatchers.IO) {
        val casesDir = File(context.filesDir, "cases")
        if (!casesDir.exists()) return@withContext emptyList()
        
        val cases = mutableListOf<Case>()
        casesDir.listFiles()?.forEach { caseDir ->
            if (caseDir.isDirectory) {
                loadCase(caseDir.name)?.let { cases.add(it) }
            }
        }
        cases.sortedByDescending { it.createdAt }
    }
}
