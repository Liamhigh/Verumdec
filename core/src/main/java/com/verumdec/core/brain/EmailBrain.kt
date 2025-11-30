package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.DateUtils
import com.verumdec.core.util.HashUtils
import com.verumdec.core.util.MetadataUtils
import java.util.*

/**
 * EmailBrain - Analyzes email evidence.
 * Handles email exports and message threads.
 */
class EmailBrain(context: Context) : BaseBrain(context) {

    override val brainName = "EmailBrain"

    /**
     * Analyze email content.
     */
    fun analyze(content: String, fileName: String): BrainResult<EmailAnalysis> {
        val hash = HashUtils.sha512(content)
        val metadata = mapOf(
            "fileName" to fileName,
            "sha512Hash" to hash,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(content, "ANALYZE_EMAIL", metadata) { text ->
            val parsed = parseEmail(text)
            
            EmailAnalysis(
                id = generateProcessingId(),
                fileName = fileName,
                contentHash = hash,
                subject = parsed.subject,
                from = parsed.from,
                to = parsed.to,
                cc = parsed.cc,
                date = parsed.date,
                body = parsed.body,
                hasAttachments = parsed.hasAttachments,
                attachmentNames = parsed.attachmentNames,
                isReply = parsed.isReply,
                isForward = parsed.isForward,
                threadDepth = parsed.threadDepth,
                mentionedEntities = MetadataUtils.extractAllEmails(text) + extractNames(text),
                extractedDates = MetadataUtils.extractDates(text),
                extractedAmounts = MetadataUtils.extractMonetaryAmounts(text),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Analyze email thread.
     */
    fun analyzeThread(content: String): BrainResult<EmailThreadAnalysis> {
        val metadata = mapOf(
            "sha512Hash" to HashUtils.sha512(content),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(content, "ANALYZE_THREAD", metadata) { text ->
            val emails = splitThread(text)
            val participants = mutableSetOf<String>()
            var firstDate: Date? = null
            var lastDate: Date? = null

            emails.forEach { email ->
                participants.add(email.from)
                email.to.forEach { participants.add(it) }
                
                email.date?.let { date ->
                    if (firstDate == null || date.before(firstDate)) firstDate = date
                    if (lastDate == null || date.after(lastDate)) lastDate = date
                }
            }

            EmailThreadAnalysis(
                id = generateProcessingId(),
                messageCount = emails.size,
                participants = participants.toList(),
                participantCount = participants.size,
                firstMessageDate = firstDate,
                lastMessageDate = lastDate,
                threadDurationMs = if (firstDate != null && lastDate != null) {
                    lastDate!!.time - firstDate!!.time
                } else 0,
                emails = emails,
                analyzedAt = Date()
            )
        }
    }

    /**
     * Extract communication patterns.
     */
    fun extractPatterns(emails: List<ParsedEmail>): BrainResult<EmailPatternAnalysis> {
        val metadata = mapOf(
            "emailCount" to emails.size,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(emails, "EXTRACT_PATTERNS", metadata) { emailList ->
            val senderFrequency = emailList.groupBy { it.from }.mapValues { it.value.size }
            val averageResponseTime = calculateAverageResponseTime(emailList)
            
            EmailPatternAnalysis(
                id = generateProcessingId(),
                totalEmails = emailList.size,
                uniqueSenders = senderFrequency.keys.toList(),
                senderFrequency = senderFrequency,
                averageResponseTimeMs = averageResponseTime,
                hasLongGaps = hasLongCommunicationGaps(emailList),
                sentimentTrend = "neutral", // Would need NLP
                analyzedAt = Date()
            )
        }
    }

    private fun parseEmail(content: String): ParsedEmail {
        val lines = content.lines()
        var subject = ""
        var from = ""
        val to = mutableListOf<String>()
        val cc = mutableListOf<String>()
        var date: Date? = null
        var inBody = false
        val bodyLines = mutableListOf<String>()
        val attachmentNames = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("Subject:", ignoreCase = true) -> {
                    subject = line.substringAfter(":").trim()
                }
                line.startsWith("From:", ignoreCase = true) -> {
                    from = line.substringAfter(":").trim()
                }
                line.startsWith("To:", ignoreCase = true) -> {
                    to.addAll(line.substringAfter(":").split(",").map { it.trim() })
                }
                line.startsWith("Cc:", ignoreCase = true) || line.startsWith("CC:", ignoreCase = true) -> {
                    cc.addAll(line.substringAfter(":").split(",").map { it.trim() })
                }
                line.startsWith("Date:", ignoreCase = true) -> {
                    date = DateUtils.parseDate(line.substringAfter(":").trim())
                }
                line.startsWith("Attachment:", ignoreCase = true) || 
                    line.contains("attached", ignoreCase = true) -> {
                    val attachmentMatch = Regex("\\[(.+?)\\]|\"(.+?)\"").find(line)
                    attachmentMatch?.let { 
                        attachmentNames.add(it.groupValues[1].ifEmpty { it.groupValues[2] })
                    }
                }
                line.isBlank() && !inBody -> {
                    inBody = true
                }
                inBody -> {
                    bodyLines.add(line)
                }
            }
        }

        val body = bodyLines.joinToString("\n")
        val isReply = subject.startsWith("Re:", ignoreCase = true)
        val isForward = subject.startsWith("Fwd:", ignoreCase = true) || 
                        subject.startsWith("Fw:", ignoreCase = true)
        val threadDepth = Regex("^(Re:\\s*)+", RegexOption.IGNORE_CASE)
            .find(subject)?.value?.split(Regex("Re:", RegexOption.IGNORE_CASE))?.size ?: 0

        return ParsedEmail(
            subject = subject,
            from = from,
            to = to,
            cc = cc,
            date = date,
            body = body,
            hasAttachments = attachmentNames.isNotEmpty(),
            attachmentNames = attachmentNames,
            isReply = isReply,
            isForward = isForward,
            threadDepth = threadDepth
        )
    }

    private fun splitThread(content: String): List<ParsedEmail> {
        val emails = mutableListOf<ParsedEmail>()
        val separatorPattern = Regex("(-{3,}|_{3,}|={3,}|On .+ wrote:|From:)")
        
        val parts = content.split(separatorPattern)
        for (part in parts) {
            if (part.trim().length > 50) {
                emails.add(parseEmail(part))
            }
        }
        
        return if (emails.isEmpty()) listOf(parseEmail(content)) else emails
    }

    private fun extractNames(text: String): List<String> {
        val namePattern = Regex("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+\\b")
        return namePattern.findAll(text).map { it.value }.distinct().toList()
    }

    private fun calculateAverageResponseTime(emails: List<ParsedEmail>): Long {
        val sortedEmails = emails.filter { it.date != null }.sortedBy { it.date }
        if (sortedEmails.size < 2) return 0

        var totalTime = 0L
        var count = 0
        for (i in 1 until sortedEmails.size) {
            val current = sortedEmails[i].date!!
            val previous = sortedEmails[i - 1].date!!
            totalTime += current.time - previous.time
            count++
        }
        
        return if (count > 0) totalTime / count else 0
    }

    private fun hasLongCommunicationGaps(emails: List<ParsedEmail>): Boolean {
        val sortedEmails = emails.filter { it.date != null }.sortedBy { it.date }
        if (sortedEmails.size < 2) return false

        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        for (i in 1 until sortedEmails.size) {
            val current = sortedEmails[i].date!!
            val previous = sortedEmails[i - 1].date!!
            if (current.time - previous.time > sevenDaysMs) {
                return true
            }
        }
        return false
    }
}

data class ParsedEmail(
    val subject: String,
    val from: String,
    val to: List<String>,
    val cc: List<String>,
    val date: Date?,
    val body: String,
    val hasAttachments: Boolean,
    val attachmentNames: List<String>,
    val isReply: Boolean,
    val isForward: Boolean,
    val threadDepth: Int
)

data class EmailAnalysis(
    val id: String,
    val fileName: String,
    val contentHash: String,
    val subject: String,
    val from: String,
    val to: List<String>,
    val cc: List<String>,
    val date: Date?,
    val body: String,
    val hasAttachments: Boolean,
    val attachmentNames: List<String>,
    val isReply: Boolean,
    val isForward: Boolean,
    val threadDepth: Int,
    val mentionedEntities: List<String>,
    val extractedDates: List<String>,
    val extractedAmounts: List<String>,
    val analyzedAt: Date
)

data class EmailThreadAnalysis(
    val id: String,
    val messageCount: Int,
    val participants: List<String>,
    val participantCount: Int,
    val firstMessageDate: Date?,
    val lastMessageDate: Date?,
    val threadDurationMs: Long,
    val emails: List<ParsedEmail>,
    val analyzedAt: Date
)

data class EmailPatternAnalysis(
    val id: String,
    val totalEmails: Int,
    val uniqueSenders: List<String>,
    val senderFrequency: Map<String, Int>,
    val averageResponseTimeMs: Long,
    val hasLongGaps: Boolean,
    val sentimentTrend: String,
    val analyzedAt: Date
)
