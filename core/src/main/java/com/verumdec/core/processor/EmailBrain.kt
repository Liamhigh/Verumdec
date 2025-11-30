package com.verumdec.core.processor

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * EmailBrain - Header parsing + spoof detection
 *
 * Analyzes email content to parse headers, detect spoofing attempts,
 * and verify email authenticity for forensic purposes.
 *
 * Operates fully offline without external dependencies.
 */
class EmailBrain {

    companion object {
        // Common email header fields
        private val REQUIRED_HEADERS = listOf("from", "to", "date", "subject")
        private val AUTHENTICATION_HEADERS = listOf("received-spf", "dkim-signature", "authentication-results", "arc-seal")

        // Time constants for validation
        private const val MS_PER_DAY = 24 * 60 * 60 * 1000L
        private const val DAYS_PER_YEAR = 365L
        private const val MS_PER_YEAR = DAYS_PER_YEAR * MS_PER_DAY

        // Common freemail domains
        private val FREEMAIL_DOMAINS = setOf(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "aol.com",
            "icloud.com", "mail.com", "protonmail.com", "zoho.com", "yandex.com"
        )

        // Suspicious patterns in display names
        private val SUSPICIOUS_DISPLAY_PATTERNS = listOf(
            Regex("\\.(com|net|org|io)\\s*$", RegexOption.IGNORE_CASE),
            Regex("@[a-z]+\\.[a-z]+", RegexOption.IGNORE_CASE),
            Regex("\\b(admin|support|security|account|verify|urgent)\\b", RegexOption.IGNORE_CASE)
        )
    }

    /**
     * Analyze an email for authenticity and spoofing.
     *
     * @param emailContent The raw email content including headers
     * @return EmailBrainResult containing analysis or error
     */
    fun analyze(emailContent: String): EmailBrainResult {
        return try {
            if (emailContent.isBlank()) {
                return EmailBrainResult.Failure(
                    error = "Empty email content provided",
                    errorCode = EmailErrorCode.INVALID_FORMAT
                )
            }

            val headers = parseHeaders(emailContent)
            if (headers.from.isBlank()) {
                return EmailBrainResult.Failure(
                    error = "Unable to parse email headers - From header missing",
                    errorCode = EmailErrorCode.HEADER_MISSING
                )
            }

            val authentication = analyzeAuthentication(emailContent)
            val spoofAnalysis = detectSpoofing(headers, emailContent)
            val threadInfo = analyzeThread(headers)
            val attachments = extractAttachmentInfo(emailContent)
            val warnings = generateWarnings(headers, spoofAnalysis, authentication)
            val trustScore = calculateTrustScore(authentication, spoofAnalysis)

            EmailBrainResult.Success(
                headers = headers,
                authentication = authentication,
                spoofAnalysis = spoofAnalysis,
                threadInfo = threadInfo,
                attachments = attachments,
                warnings = warnings,
                trustScore = trustScore
            )
        } catch (e: Exception) {
            EmailBrainResult.Failure(
                error = "Email analysis error: ${e.message}",
                errorCode = EmailErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Parse email headers from content.
     */
    private fun parseHeaders(content: String): EmailHeaders {
        val headerSection = extractHeaderSection(content)
        val headerMap = parseHeaderMap(headerSection)

        // Parse From header
        val from = headerMap["from"] ?: ""

        // Parse To header (can be multiple)
        val to = parseAddressList(headerMap["to"] ?: "")

        // Parse CC header
        val cc = parseAddressList(headerMap["cc"] ?: "")

        // Parse BCC header (usually not present in received emails)
        val bcc = parseAddressList(headerMap["bcc"] ?: "")

        // Parse Subject
        val subject = decodeHeader(headerMap["subject"] ?: "")

        // Parse Date
        val date = parseEmailDate(headerMap["date"] ?: "")

        // Parse Message-ID
        val messageId = extractMessageId(headerMap["message-id"] ?: "")

        // Parse In-Reply-To
        val inReplyTo = extractMessageId(headerMap["in-reply-to"] ?: "")

        // Parse References
        val references = parseReferences(headerMap["references"] ?: "")

        // Parse Received chain
        val receivedChain = parseReceivedChain(content)

        // Collect custom headers
        val customHeaders = headerMap.filterKeys { key ->
            key !in listOf("from", "to", "cc", "bcc", "subject", "date", "message-id", "in-reply-to", "references")
        }.mapValues { decodeHeader(it.value) }

        return EmailHeaders(
            from = from,
            to = to,
            cc = cc,
            bcc = bcc,
            subject = subject,
            date = date,
            messageId = messageId,
            inReplyTo = inReplyTo,
            references = references,
            receivedChain = receivedChain,
            customHeaders = customHeaders
        )
    }

    /**
     * Extract header section from email content.
     */
    private fun extractHeaderSection(content: String): String {
        // Headers end at first blank line
        val blankLineIndex = content.indexOf("\n\n")
        return if (blankLineIndex > 0) {
            content.substring(0, blankLineIndex)
        } else {
            content.take(4096) // Limit header parsing
        }
    }

    /**
     * Parse headers into a map.
     */
    private fun parseHeaderMap(headerSection: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val lines = headerSection.lines()
        var currentHeader = ""
        var currentValue = StringBuilder()

        for (line in lines) {
            if (line.isBlank()) continue

            if (line.startsWith(" ") || line.startsWith("\t")) {
                // Continuation of previous header
                currentValue.append(" ").append(line.trim())
            } else {
                // New header
                if (currentHeader.isNotEmpty()) {
                    headers[currentHeader.lowercase()] = currentValue.toString().trim()
                }

                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    currentHeader = line.substring(0, colonIndex).trim()
                    currentValue = StringBuilder(line.substring(colonIndex + 1).trim())
                }
            }
        }

        // Add last header
        if (currentHeader.isNotEmpty()) {
            headers[currentHeader.lowercase()] = currentValue.toString().trim()
        }

        return headers
    }

    /**
     * Parse address list from header value.
     */
    private fun parseAddressList(value: String): List<String> {
        if (value.isBlank()) return emptyList()

        return value.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Decode MIME encoded header.
     */
    private fun decodeHeader(value: String): String {
        // Basic MIME decoding (=?charset?encoding?text?=)
        val mimePattern = Regex("=\\?([^?]+)\\?([BbQq])\\?([^?]+)\\?=")
        var decoded = value

        mimePattern.findAll(value).forEach { match ->
            val charset = match.groupValues[1]
            val encoding = match.groupValues[2].uppercase()
            val encodedText = match.groupValues[3]

            val decodedText = try {
                when (encoding) {
                    "B" -> String(java.util.Base64.getDecoder().decode(encodedText), charset(charset))
                    "Q" -> decodeQuotedPrintable(encodedText)
                    else -> encodedText
                }
            } catch (e: Exception) {
                encodedText
            }

            decoded = decoded.replace(match.value, decodedText)
        }

        return decoded
    }

    /**
     * Decode quoted-printable text.
     */
    private fun decodeQuotedPrintable(text: String): String {
        return text
            .replace("_", " ")
            .replace(Regex("=([0-9A-Fa-f]{2})")) { match ->
                val hex = match.groupValues[1]
                String(byteArrayOf(hex.toInt(16).toByte()))
            }
    }

    /**
     * Parse email date.
     */
    private fun parseEmailDate(dateStr: String): Date? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.US).parse(dateStr.trim())
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Extract Message-ID from header value.
     */
    private fun extractMessageId(value: String): String? {
        val match = Regex("<([^>]+)>").find(value)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Parse References header.
     */
    private fun parseReferences(value: String): List<String> {
        if (value.isBlank()) return emptyList()

        return Regex("<([^>]+)>").findAll(value)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Parse Received header chain.
     */
    private fun parseReceivedChain(content: String): List<ReceivedHeader> {
        val chain = mutableListOf<ReceivedHeader>()
        val receivedPattern = Regex("Received:\\s*(.+?)(?=\\nReceived:|\\n[A-Z][a-z-]+:|\n\n)", RegexOption.DOT_MATCHES_ALL)

        receivedPattern.findAll(content).forEach { match ->
            val receivedValue = match.groupValues[1].trim()
            val parsed = parseReceivedHeader(receivedValue)
            chain.add(parsed)
        }

        return chain
    }

    /**
     * Parse a single Received header.
     */
    private fun parseReceivedHeader(value: String): ReceivedHeader {
        val fromMatch = Regex("from\\s+([^\\s(]+)").find(value)
        val byMatch = Regex("by\\s+([^\\s(]+)").find(value)
        val dateMatch = Regex(";\\s*(.+)$").find(value)

        val date = dateMatch?.groupValues?.getOrNull(1)?.let { parseEmailDate(it.trim()) }

        return ReceivedHeader(
            from = fromMatch?.groupValues?.getOrNull(1) ?: "",
            by = byMatch?.groupValues?.getOrNull(1) ?: "",
            date = date,
            raw = value.take(500)
        )
    }

    /**
     * Analyze email authentication headers.
     */
    private fun analyzeAuthentication(content: String): EmailAuthentication {
        val headerMap = parseHeaderMap(extractHeaderSection(content))

        // Check SPF
        val spfResult = parseAuthResult(headerMap["received-spf"] ?: "", headerMap["authentication-results"] ?: "", "spf")

        // Check DKIM
        val dkimResult = parseAuthResult(headerMap["dkim-signature"]?.let { "pass" } ?: "", headerMap["authentication-results"] ?: "", "dkim")

        // Check DMARC
        val dmarcResult = parseAuthResult("", headerMap["authentication-results"] ?: "", "dmarc")

        val overallAuthenticated = spfResult == AuthResult.PASS &&
                dkimResult == AuthResult.PASS &&
                dmarcResult == AuthResult.PASS

        return EmailAuthentication(
            spfResult = spfResult,
            dkimResult = dkimResult,
            dmarcResult = dmarcResult,
            overallAuthenticated = overallAuthenticated
        )
    }

    /**
     * Parse authentication result from headers.
     */
    private fun parseAuthResult(dedicatedHeader: String, authResults: String, authType: String): AuthResult {
        val lowerHeader = dedicatedHeader.lowercase()
        val lowerAuthResults = authResults.lowercase()

        // Check dedicated header first
        if (lowerHeader.isNotBlank()) {
            return when {
                lowerHeader.contains("pass") -> AuthResult.PASS
                lowerHeader.contains("fail") -> AuthResult.FAIL
                lowerHeader.contains("softfail") -> AuthResult.SOFTFAIL
                lowerHeader.contains("neutral") -> AuthResult.NEUTRAL
                else -> AuthResult.UNKNOWN
            }
        }

        // Check Authentication-Results header
        val pattern = Regex("$authType=([a-z]+)", RegexOption.IGNORE_CASE)
        val match = pattern.find(lowerAuthResults)

        return when (match?.groupValues?.getOrNull(1)?.lowercase()) {
            "pass" -> AuthResult.PASS
            "fail" -> AuthResult.FAIL
            "softfail" -> AuthResult.SOFTFAIL
            "neutral" -> AuthResult.NEUTRAL
            "none" -> AuthResult.NONE
            else -> AuthResult.NONE
        }
    }

    /**
     * Detect spoofing attempts.
     */
    private fun detectSpoofing(headers: EmailHeaders, content: String): SpoofAnalysis {
        val indicators = mutableListOf<SpoofIndicator>()
        var spoofConfidence = 0f

        // Check display name deception
        val displayNameDeception = checkDisplayNameDeception(headers.from)
        if (displayNameDeception) {
            indicators.add(SpoofIndicator(
                type = SpoofIndicatorType.DISPLAY_NAME_DECEPTION,
                description = "Display name contains suspicious patterns (domain-like or impersonation keywords)",
                severity = AnomalySeverity.HIGH
            ))
            spoofConfidence += 0.3f
        }

        // Check from domain mismatch
        val fromDomainMismatch = checkDomainMismatch(headers)
        if (fromDomainMismatch) {
            indicators.add(SpoofIndicator(
                type = SpoofIndicatorType.DOMAIN_MISMATCH,
                description = "From domain does not match Reply-To or Return-Path domain",
                severity = AnomalySeverity.HIGH
            ))
            spoofConfidence += 0.25f
        }

        // Check Reply-To mismatch
        val replyToMismatch = checkReplyToMismatch(headers)
        if (replyToMismatch) {
            indicators.add(SpoofIndicator(
                type = SpoofIndicatorType.REPLY_TO_MISMATCH,
                description = "Reply-To address differs from From address",
                severity = AnomalySeverity.MEDIUM
            ))
            spoofConfidence += 0.15f
        }

        // Check received chain anomalies
        val receivedAnomaly = checkReceivedChainAnomaly(headers.receivedChain)
        if (receivedAnomaly != null) {
            indicators.add(SpoofIndicator(
                type = SpoofIndicatorType.RECEIVED_CHAIN_ANOMALY,
                description = receivedAnomaly,
                severity = AnomalySeverity.MEDIUM
            ))
            spoofConfidence += 0.15f
        }

        // Check timestamp anomalies
        val timestampAnomaly = checkTimestampAnomaly(headers)
        if (timestampAnomaly) {
            indicators.add(SpoofIndicator(
                type = SpoofIndicatorType.TIMESTAMP_ANOMALY,
                description = "Email date is in the future or unreasonably old",
                severity = AnomalySeverity.MEDIUM
            ))
            spoofConfidence += 0.1f
        }

        // Check for missing authentication
        val headerMap = parseHeaderMap(extractHeaderSection(content))
        if (!headerMap.keys.any { it in AUTHENTICATION_HEADERS }) {
            indicators.add(SpoofIndicator(
                type = SpoofIndicatorType.MISSING_AUTHENTICATION,
                description = "No authentication headers (SPF, DKIM, DMARC) found",
                severity = AnomalySeverity.LOW
            ))
            spoofConfidence += 0.05f
        }

        val spoofDetected = indicators.any { it.severity == AnomalySeverity.HIGH } ||
                indicators.size >= 3 ||
                spoofConfidence >= 0.5f

        return SpoofAnalysis(
            spoofDetected = spoofDetected,
            spoofConfidence = spoofConfidence.coerceIn(0f, 1f),
            indicators = indicators,
            fromDomainMismatch = fromDomainMismatch,
            replyToMismatch = replyToMismatch,
            displayNameDeception = displayNameDeception
        )
    }

    /**
     * Check for display name deception.
     */
    private fun checkDisplayNameDeception(from: String): Boolean {
        // Extract display name (before the email address)
        val displayNameMatch = Regex("^([^<]+)<").find(from)
        val displayName = displayNameMatch?.groupValues?.getOrNull(1)?.trim() ?: return false

        // Check for suspicious patterns
        return SUSPICIOUS_DISPLAY_PATTERNS.any { it.containsMatchIn(displayName) }
    }

    /**
     * Check for domain mismatch.
     */
    private fun checkDomainMismatch(headers: EmailHeaders): Boolean {
        val fromDomain = extractDomain(headers.from) ?: return false

        // Check Return-Path if present
        val returnPath = headers.customHeaders["return-path"]
        if (returnPath != null) {
            val returnPathDomain = extractDomain(returnPath)
            if (returnPathDomain != null && !domainsMatch(fromDomain, returnPathDomain)) {
                return true
            }
        }

        return false
    }

    /**
     * Check for Reply-To mismatch.
     */
    private fun checkReplyToMismatch(headers: EmailHeaders): Boolean {
        val replyTo = headers.customHeaders["reply-to"] ?: return false
        val fromEmail = extractEmailAddress(headers.from)
        val replyToEmail = extractEmailAddress(replyTo)

        return fromEmail != replyToEmail
    }

    /**
     * Check for received chain anomalies.
     */
    private fun checkReceivedChainAnomaly(chain: List<ReceivedHeader>): String? {
        if (chain.isEmpty()) {
            return "No Received headers found - email may not have been transmitted normally"
        }

        // Check for timestamp reversals
        for (i in 0 until chain.size - 1) {
            val current = chain[i].date
            val next = chain[i + 1].date
            if (current != null && next != null && current.before(next)) {
                return "Timestamp reversal detected in Received chain"
            }
        }

        // Check for private IP addresses in public chain
        val privateIpPattern = Regex("\\b(10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.)")
        for (header in chain) {
            if (privateIpPattern.containsMatchIn(header.from) && chain.size > 2) {
                // Private IPs should only appear at the start
                val index = chain.indexOf(header)
                if (index > 1) {
                    return "Private IP address found in unexpected position in Received chain"
                }
            }
        }

        return null
    }

    /**
     * Check for timestamp anomalies.
     */
    private fun checkTimestampAnomaly(headers: EmailHeaders): Boolean {
        val date = headers.date ?: return false
        val now = Date()

        // Check if date is in the future (with 1 day tolerance)
        if (date.after(Date(now.time + MS_PER_DAY))) {
            return true
        }

        // Check if date is unreasonably old (more than 1 year)
        if (date.before(Date(now.time - MS_PER_YEAR))) {
            return true
        }

        return false
    }

    /**
     * Extract domain from email address.
     */
    private fun extractDomain(address: String): String? {
        val email = extractEmailAddress(address)
        val atIndex = email.indexOf('@')
        return if (atIndex > 0) email.substring(atIndex + 1).lowercase() else null
    }

    /**
     * Extract email address from header value.
     */
    private fun extractEmailAddress(value: String): String {
        val match = Regex("<([^>]+)>").find(value)
        return match?.groupValues?.getOrNull(1)?.lowercase()?.trim()
            ?: value.lowercase().trim()
    }

    /**
     * Check if two domains match (considering subdomains).
     */
    private fun domainsMatch(domain1: String, domain2: String): Boolean {
        if (domain1 == domain2) return true

        // Check if one is a subdomain of the other
        return domain1.endsWith(".$domain2") || domain2.endsWith(".$domain1")
    }

    /**
     * Analyze email thread information.
     */
    private fun analyzeThread(headers: EmailHeaders): EmailThreadInfo {
        val isReply = headers.inReplyTo != null || headers.subject.lowercase().startsWith("re:")
        val isForward = headers.subject.lowercase().startsWith("fw:") ||
                headers.subject.lowercase().startsWith("fwd:")

        val threadDepth = headers.references.size + (if (isReply) 1 else 0)

        // Extract original sender from references or In-Reply-To
        val originalSender = if (headers.references.isNotEmpty()) {
            // First reference is usually the original message
            null // Would need to look up the original sender
        } else {
            null
        }

        // Collect thread participants from To and CC
        val participants = (headers.to + headers.cc)
            .map { extractEmailAddress(it) }
            .distinct()

        return EmailThreadInfo(
            isReply = isReply,
            isForward = isForward,
            threadDepth = threadDepth,
            originalSender = originalSender,
            threadParticipants = participants
        )
    }

    /**
     * Extract attachment information from email.
     */
    private fun extractAttachmentInfo(content: String): List<EmailAttachment> {
        val attachments = mutableListOf<EmailAttachment>()

        // Look for Content-Disposition: attachment headers
        val attachmentPattern = Regex(
            "Content-Disposition:\\s*attachment[^\\n]*filename[=\"']*([^\"'\\n]+)",
            RegexOption.IGNORE_CASE
        )

        attachmentPattern.findAll(content).forEach { match ->
            val filename = match.groupValues[1].trim()
            if (filename.isNotBlank()) {
                val mimeType = guessMimeType(filename)
                attachments.add(EmailAttachment(
                    filename = filename,
                    mimeType = mimeType,
                    size = 0, // Cannot determine from text content
                    hash = "" // Cannot compute from text content
                ))
            }
        }

        return attachments
    }

    /**
     * Guess MIME type from filename.
     */
    private fun guessMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            else -> "application/octet-stream"
        }
    }

    /**
     * Generate warnings based on analysis.
     */
    private fun generateWarnings(
        headers: EmailHeaders,
        spoofAnalysis: SpoofAnalysis,
        authentication: EmailAuthentication
    ): List<String> {
        val warnings = mutableListOf<String>()

        if (spoofAnalysis.spoofDetected) {
            warnings.add("CRITICAL: Email spoofing detected - treat with extreme caution")
        }

        for (indicator in spoofAnalysis.indicators) {
            if (indicator.severity in listOf(AnomalySeverity.CRITICAL, AnomalySeverity.HIGH)) {
                warnings.add("${indicator.severity}: ${indicator.description}")
            }
        }

        if (!authentication.overallAuthenticated) {
            val failedChecks = mutableListOf<String>()
            if (authentication.spfResult != AuthResult.PASS) failedChecks.add("SPF")
            if (authentication.dkimResult != AuthResult.PASS) failedChecks.add("DKIM")
            if (authentication.dmarcResult != AuthResult.PASS) failedChecks.add("DMARC")

            if (failedChecks.isNotEmpty()) {
                warnings.add("Authentication checks not passed: ${failedChecks.joinToString(", ")}")
            }
        }

        if (headers.receivedChain.isEmpty()) {
            warnings.add("No Received headers - email transmission path cannot be verified")
        }

        return warnings
    }

    /**
     * Calculate overall trust score.
     */
    private fun calculateTrustScore(
        authentication: EmailAuthentication,
        spoofAnalysis: SpoofAnalysis
    ): Float {
        var score = 100f

        // Authentication impact
        if (authentication.spfResult != AuthResult.PASS) score -= 15f
        if (authentication.dkimResult != AuthResult.PASS) score -= 15f
        if (authentication.dmarcResult != AuthResult.PASS) score -= 10f

        // Spoof indicators impact
        for (indicator in spoofAnalysis.indicators) {
            score -= when (indicator.severity) {
                AnomalySeverity.CRITICAL -> 25f
                AnomalySeverity.HIGH -> 15f
                AnomalySeverity.MEDIUM -> 10f
                AnomalySeverity.LOW -> 5f
                AnomalySeverity.INFO -> 2f
            }
        }

        // Major red flags
        if (spoofAnalysis.spoofDetected) score -= 20f

        return score.coerceIn(0f, 100f)
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: EmailBrainResult): String {
        return when (result) {
            is EmailBrainResult.Success -> buildSuccessJson(result)
            is EmailBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: EmailBrainResult.Success): String {
        val toJson = result.headers.to.joinToString(",") { "\"${escapeJson(it)}\"" }
        val ccJson = result.headers.cc.joinToString(",") { "\"${escapeJson(it)}\"" }
        val bccJson = result.headers.bcc.joinToString(",") { "\"${escapeJson(it)}\"" }
        val referencesJson = result.headers.references.joinToString(",") { "\"${escapeJson(it)}\"" }
        val receivedChainJson = result.headers.receivedChain.joinToString(",") { r ->
            """{"from":"${escapeJson(r.from)}","by":"${escapeJson(r.by)}","date":${r.date?.time ?: "null"},"raw":"${escapeJson(r.raw.take(200))}"}"""
        }
        val customHeadersJson = result.headers.customHeaders.entries.joinToString(",") {
            "\"${escapeJson(it.key)}\":\"${escapeJson(it.value.take(200))}\""
        }
        val indicatorsJson = result.spoofAnalysis.indicators.joinToString(",") { i ->
            """{"type":"${i.type}","description":"${escapeJson(i.description)}","severity":"${i.severity}"}"""
        }
        val participantsJson = result.threadInfo.threadParticipants.joinToString(",") { "\"${escapeJson(it)}\"" }
        val attachmentsJson = result.attachments.joinToString(",") { a ->
            """{"filename":"${escapeJson(a.filename)}","mimeType":"${a.mimeType}","size":${a.size},"hash":"${a.hash}"}"""
        }
        val warningsJson = result.warnings.joinToString(",") { "\"${escapeJson(it)}\"" }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "emailId": "${result.emailId}",
            "headers": {
                "from": "${escapeJson(result.headers.from)}",
                "to": [$toJson],
                "cc": [$ccJson],
                "bcc": [$bccJson],
                "subject": "${escapeJson(result.headers.subject)}",
                "date": ${result.headers.date?.time ?: "null"},
                "messageId": ${result.headers.messageId?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "inReplyTo": ${result.headers.inReplyTo?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "references": [$referencesJson],
                "receivedChain": [$receivedChainJson],
                "customHeaders": {$customHeadersJson}
            },
            "authentication": {
                "spfResult": "${result.authentication.spfResult}",
                "dkimResult": "${result.authentication.dkimResult}",
                "dmarcResult": "${result.authentication.dmarcResult}",
                "overallAuthenticated": ${result.authentication.overallAuthenticated}
            },
            "spoofAnalysis": {
                "spoofDetected": ${result.spoofAnalysis.spoofDetected},
                "spoofConfidence": ${result.spoofAnalysis.spoofConfidence},
                "indicators": [$indicatorsJson],
                "fromDomainMismatch": ${result.spoofAnalysis.fromDomainMismatch},
                "replyToMismatch": ${result.spoofAnalysis.replyToMismatch},
                "displayNameDeception": ${result.spoofAnalysis.displayNameDeception}
            },
            "threadInfo": {
                "isReply": ${result.threadInfo.isReply},
                "isForward": ${result.threadInfo.isForward},
                "threadDepth": ${result.threadInfo.threadDepth},
                "originalSender": ${result.threadInfo.originalSender?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "threadParticipants": [$participantsJson]
            },
            "attachments": [$attachmentsJson],
            "warnings": [$warningsJson],
            "trustScore": ${result.trustScore}
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: EmailBrainResult.Failure): String {
        return """
        {
            "success": false,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "error": "${escapeJson(result.error)}",
            "errorCode": "${result.errorCode}"
        }
        """.trimIndent()
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
