package com.verumdec.engine

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Multi-Jurisdiction Compliance Engine for Verum Omnis
 *
 * Provides compliance with forensic standards across multiple jurisdictions:
 * - UAE: Arabic text support, right-to-left layout
 * - SA (South Africa): ECT Act timestamps
 * - EU: GDPR data handling
 * - US: Federal Rules of Evidence formatting
 *
 * This enables court admissibility across different legal systems.
 */
class JurisdictionComplianceEngine {

    companion object {
        val SUPPORTED_JURISDICTIONS = listOf(
            Jurisdiction.UAE,
            Jurisdiction.SOUTH_AFRICA,
            Jurisdiction.EUROPEAN_UNION,
            Jurisdiction.UNITED_STATES
        )
    }

    /**
     * Gets the compliance configuration for a jurisdiction
     */
    fun getComplianceConfig(jurisdiction: Jurisdiction): JurisdictionConfig {
        return when (jurisdiction) {
            Jurisdiction.UAE -> UAEConfig()
            Jurisdiction.SOUTH_AFRICA -> SouthAfricaConfig()
            Jurisdiction.EUROPEAN_UNION -> EUConfig()
            Jurisdiction.UNITED_STATES -> USConfig()
        }
    }

    /**
     * Formats a timestamp according to jurisdiction requirements
     */
    fun formatTimestamp(timestamp: Instant, jurisdiction: Jurisdiction): String {
        val config = getComplianceConfig(jurisdiction)
        return config.timestampFormatter.format(timestamp)
    }

    /**
     * Gets the legal disclaimer for a jurisdiction
     */
    fun getLegalDisclaimer(jurisdiction: Jurisdiction): String {
        return getComplianceConfig(jurisdiction).legalDisclaimer
    }

    /**
     * Gets the evidence handling standards for a jurisdiction
     */
    fun getEvidenceStandards(jurisdiction: Jurisdiction): List<String> {
        return getComplianceConfig(jurisdiction).evidenceStandards
    }

    /**
     * Generates a jurisdiction-specific footer block
     */
    fun generateFooter(
        jurisdiction: Jurisdiction,
        caseName: String,
        hash: String,
        timestamp: Instant,
        deviceInfo: String
    ): String {
        val config = getComplianceConfig(jurisdiction)
        return config.generateFooter(caseName, hash, timestamp, deviceInfo)
    }

    /**
     * Checks if a jurisdiction requires right-to-left layout
     */
    fun requiresRtlLayout(jurisdiction: Jurisdiction): Boolean {
        return getComplianceConfig(jurisdiction).isRtl
    }

    /**
     * Gets the primary language for a jurisdiction
     */
    fun getPrimaryLanguage(jurisdiction: Jurisdiction): Locale {
        return getComplianceConfig(jurisdiction).primaryLocale
    }
}

/**
 * Supported jurisdictions
 */
enum class Jurisdiction {
    UAE,
    SOUTH_AFRICA,
    EUROPEAN_UNION,
    UNITED_STATES
}

/**
 * Base configuration for jurisdiction compliance
 */
abstract class JurisdictionConfig {
    abstract val name: String
    abstract val code: String
    abstract val primaryLocale: Locale
    abstract val isRtl: Boolean
    abstract val timestampFormatter: DateTimeFormatter
    abstract val legalDisclaimer: String
    abstract val evidenceStandards: List<String>
    abstract val dataProtectionAct: String

    abstract fun generateFooter(
        caseName: String,
        hash: String,
        timestamp: Instant,
        deviceInfo: String
    ): String
}

/**
 * UAE Jurisdiction Configuration
 *
 * Features:
 * - Arabic language support
 * - Right-to-left layout
 * - UAE Federal Evidence Law compliance
 */
class UAEConfig : JurisdictionConfig() {
    override val name = "United Arab Emirates"
    override val code = "UAE"
    override val primaryLocale: Locale = Locale("ar", "AE")
    override val isRtl = true
    override val timestampFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        .withZone(ZoneId.of("Asia/Dubai"))
    override val legalDisclaimer = """
        |هذا التقرير صادر وفقاً لقانون الإثبات الاتحادي لدولة الإمارات العربية المتحدة
        |This report is generated in compliance with UAE Federal Evidence Law
        |Article 17: Electronic Evidence Admissibility Standards
        """.trimMargin()
    override val evidenceStandards = listOf(
        "UAE Federal Law No. 1 of 2006 - Electronic Transactions and Commerce Law",
        "UAE Evidence Law - Article 17 Electronic Evidence",
        "Dubai Electronic Security Center Standards"
    )
    override val dataProtectionAct = "UAE Data Protection Law (Federal Decree-Law No. 45/2021)"

    override fun generateFooter(
        caseName: String,
        hash: String,
        timestamp: Instant,
        deviceInfo: String
    ): String = buildString {
        appendLine("═".repeat(72))
        appendLine("FORENSIC EVIDENCE SEAL - UAE JURISDICTION")
        appendLine("ختم الأدلة الجنائية - الولاية القضائية الإماراتية")
        appendLine("═".repeat(72))
        appendLine("Case / القضية: $caseName")
        appendLine("Hash / التجزئة: SHA512-${hash.take(64)}")
        appendLine("Timestamp / الطابع الزمني: ${timestampFormatter.format(timestamp)}")
        appendLine("Device / الجهاز: $deviceInfo")
        appendLine("Standard / المعيار: UAE Federal Evidence Law")
        appendLine("═".repeat(72))
    }
}

/**
 * South Africa Jurisdiction Configuration
 *
 * Features:
 * - ECT Act (Electronic Communications and Transactions Act) compliance
 * - SAPS (South African Police Service) evidence standards
 */
class SouthAfricaConfig : JurisdictionConfig() {
    override val name = "South Africa"
    override val code = "SA"
    override val primaryLocale: Locale = Locale("en", "ZA")
    override val isRtl = false
    override val timestampFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        .withZone(ZoneId.of("Africa/Johannesburg"))
    override val legalDisclaimer = """
        |This report is generated in compliance with the Electronic Communications
        |and Transactions Act 25 of 2002 (ECT Act) of South Africa.
        |Evidence handling follows SAPS digital evidence guidelines.
        """.trimMargin()
    override val evidenceStandards = listOf(
        "ECT Act 25 of 2002 - Section 15: Electronic Evidence Admissibility",
        "Criminal Procedure Act 51 of 1977 - Documentary Evidence",
        "SAPS Digital Evidence Collection Guidelines",
        "Protection of Personal Information Act (POPIA)"
    )
    override val dataProtectionAct = "Protection of Personal Information Act 4 of 2013 (POPIA)"

    override fun generateFooter(
        caseName: String,
        hash: String,
        timestamp: Instant,
        deviceInfo: String
    ): String = buildString {
        appendLine("═".repeat(72))
        appendLine("FORENSIC EVIDENCE SEAL - SOUTH AFRICA JURISDICTION")
        appendLine("═".repeat(72))
        appendLine("Case: $caseName")
        appendLine("Hash: SHA512-${hash.take(64)}")
        appendLine("Timestamp (SAST): ${timestampFormatter.format(timestamp)}")
        appendLine("Device: $deviceInfo")
        appendLine("Standard: ECT Act Section 15")
        appendLine("SAPS Reference: [To be assigned]")
        appendLine("═".repeat(72))
    }
}

/**
 * European Union Jurisdiction Configuration
 *
 * Features:
 * - GDPR (General Data Protection Regulation) compliance
 * - eIDAS (Electronic Identification and Trust Services) standards
 */
class EUConfig : JurisdictionConfig() {
    override val name = "European Union"
    override val code = "EU"
    override val primaryLocale: Locale = Locale.ENGLISH
    override val isRtl = false
    override val timestampFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        .withZone(ZoneId.of("Europe/Brussels"))
    override val legalDisclaimer = """
        |This report is generated in compliance with:
        |- EU Regulation 2016/679 (GDPR) - Data Protection
        |- EU Regulation 910/2014 (eIDAS) - Electronic Identification
        |- Directive 2014/41/EU - European Investigation Order
        """.trimMargin()
    override val evidenceStandards = listOf(
        "eIDAS Regulation (EU) No 910/2014 - Electronic Signatures and Seals",
        "GDPR Article 5 - Principles of Data Processing",
        "GDPR Article 17 - Right to Erasure",
        "Directive 2014/41/EU - European Investigation Order"
    )
    override val dataProtectionAct = "General Data Protection Regulation (GDPR) - Regulation (EU) 2016/679"

    override fun generateFooter(
        caseName: String,
        hash: String,
        timestamp: Instant,
        deviceInfo: String
    ): String = buildString {
        appendLine("═".repeat(72))
        appendLine("FORENSIC EVIDENCE SEAL - EU JURISDICTION")
        appendLine("═".repeat(72))
        appendLine("Case: $caseName")
        appendLine("Hash: SHA512-${hash.take(64)}")
        appendLine("Timestamp (CET/CEST): ${timestampFormatter.format(timestamp)}")
        appendLine("Device: $deviceInfo")
        appendLine("Standard: eIDAS / GDPR Compliant")
        appendLine("Data Protection: GDPR Article 5 Compliant")
        appendLine("═".repeat(72))
    }
}

/**
 * United States Jurisdiction Configuration
 *
 * Features:
 * - Federal Rules of Evidence compliance
 * - Daubert Standard methodology documentation
 * - CISA (Cybersecurity and Infrastructure Security Agency) guidelines
 */
class USConfig : JurisdictionConfig() {
    override val name = "United States"
    override val code = "US"
    override val primaryLocale: Locale = Locale.US
    override val isRtl = false
    override val timestampFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        .withZone(ZoneId.of("America/New_York"))
    override val legalDisclaimer = """
        |This report is generated in compliance with:
        |- Federal Rules of Evidence (FRE) Rule 901(b)(9) - Self-Authentication
        |- FRE Rule 902(13)-(14) - Certified Domestic Records
        |- Daubert Standard - Scientific Methodology for Court Admissibility
        """.trimMargin()
    override val evidenceStandards = listOf(
        "Federal Rules of Evidence Rule 901(b)(9) - Self-Authentication",
        "Federal Rules of Evidence Rule 902(13)-(14) - Certified Records",
        "Daubert Standard - Scientific Methodology Documentation",
        "NIST SP 800-86 - Guide to Integrating Forensic Techniques",
        "DoJ Electronic Evidence Guidelines"
    )
    override val dataProtectionAct = "Various State Privacy Laws (CCPA, etc.)"

    override fun generateFooter(
        caseName: String,
        hash: String,
        timestamp: Instant,
        deviceInfo: String
    ): String = buildString {
        appendLine("═".repeat(72))
        appendLine("FORENSIC EVIDENCE SEAL - US FEDERAL JURISDICTION")
        appendLine("═".repeat(72))
        appendLine("Case: $caseName")
        appendLine("Hash: SHA512-${hash.take(64)}")
        appendLine("Timestamp (ET): ${timestampFormatter.format(timestamp)}")
        appendLine("Device: $deviceInfo")
        appendLine("Standard: FRE 901(b)(9) / Daubert Compliant")
        appendLine("NIST Reference: SP 800-86")
        appendLine("═".repeat(72))
    }
}

/**
 * Report section with jurisdiction-specific formatting
 */
data class JurisdictionReportSection(
    val title: String,
    val titleLocalized: String?,
    val content: String,
    val isRtl: Boolean
)

/**
 * Extension to generate jurisdiction-specific compliance report section
 */
fun JurisdictionConfig.generateComplianceSection(): String = buildString {
    appendLine("═".repeat(72))
    appendLine("JURISDICTION COMPLIANCE: $name")
    appendLine("═".repeat(72))
    appendLine()
    appendLine("Legal Framework:")
    evidenceStandards.forEach { standard ->
        appendLine("  • $standard")
    }
    appendLine()
    appendLine("Data Protection:")
    appendLine("  $dataProtectionAct")
    appendLine()
    appendLine("Disclaimer:")
    appendLine(legalDisclaimer)
    appendLine()
    appendLine("═".repeat(72))
}
