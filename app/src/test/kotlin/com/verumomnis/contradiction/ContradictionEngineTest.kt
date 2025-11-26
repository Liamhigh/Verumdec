package com.verumomnis.contradiction

import com.verumomnis.contradiction.engine.*
import com.verumomnis.contradiction.nlp.NlpUtils
import com.verumomnis.contradiction.pdf.PdfSealEngine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for Verum Omnis Contradiction Engine
 */
class ContradictionEngineTest {

    @Test
    fun `test direct contradiction detection`() {
        val engine = ContradictionEngine()

        val now = LocalDateTime.now()

        engine.addClaim(
            Claim(
                id = "claim_1",
                entityId = "TestEntity",
                statement = "No deal ever existed",
                timestamp = now.minusDays(10),
                sourceDocument = "test1.pdf",
                sourceType = Claim.SourceType.PDF
            )
        )

        engine.addClaim(
            Claim(
                id = "claim_2",
                entityId = "TestEntity",
                statement = "The deal fell through",
                timestamp = now.minusDays(5),
                sourceDocument = "test2.pdf",
                sourceType = Claim.SourceType.PDF
            )
        )

        val contradictions = engine.analyzeContradictions()

        assertTrue("Should detect at least one contradiction", contradictions.isNotEmpty())
    }

    @Test
    fun `test entity registration`() {
        val engine = ContradictionEngine()

        val entity = Entity(
            id = "test_entity",
            primaryName = "Test Person",
            aliases = setOf("TP", "Tester"),
            email = "test@example.com"
        )

        engine.registerEntity(entity)

        val entities = engine.getEntities()
        assertTrue("Entity should be registered", entities.containsKey("test_entity"))
        assertEquals("Test Person", entities["test_entity"]?.primaryName)
    }

    @Test
    fun `test timeline generation`() {
        val engine = ContradictionEngine()
        val now = LocalDateTime.now()

        engine.addClaim(
            Claim(
                id = "claim_1",
                entityId = "Entity1",
                statement = "First statement",
                timestamp = now.minusDays(5),
                sourceDocument = "doc1.pdf",
                sourceType = Claim.SourceType.PDF
            )
        )

        engine.addClaim(
            Claim(
                id = "claim_2",
                entityId = "Entity2",
                statement = "Second statement",
                timestamp = now.minusDays(2),
                sourceDocument = "doc2.pdf",
                sourceType = Claim.SourceType.PDF
            )
        )

        val timeline = engine.getTimeline()

        assertEquals("Timeline should have 2 events", 2, timeline.size)
        assertTrue("First event should be earlier", timeline[0].timestamp.isBefore(timeline[1].timestamp))
    }

    @Test
    fun `test liability calculation`() {
        val engine = ContradictionEngine()
        val now = LocalDateTime.now()

        engine.registerEntity(
            Entity(id = "honest", primaryName = "Honest Person")
        )
        engine.registerEntity(
            Entity(id = "deceptive", primaryName = "Deceptive Person")
        )

        // Add consistent claims for honest entity
        engine.addClaim(
            Claim(
                id = "h1",
                entityId = "honest",
                statement = "I sent the payment on time",
                timestamp = now.minusDays(10),
                sourceDocument = "proof.pdf",
                sourceType = Claim.SourceType.PDF
            )
        )

        // Add contradictory claims for deceptive entity
        engine.addClaim(
            Claim(
                id = "d1",
                entityId = "deceptive",
                statement = "No payment was ever received",
                timestamp = now.minusDays(8),
                sourceDocument = "email1.eml",
                sourceType = Claim.SourceType.EMAIL
            )
        )

        engine.addClaim(
            Claim(
                id = "d2",
                entityId = "deceptive",
                statement = "The payment was received but late",
                timestamp = now.minusDays(3),
                sourceDocument = "email2.eml",
                sourceType = Claim.SourceType.EMAIL
            )
        )

        engine.analyzeContradictions()
        engine.analyzeBehavior()
        val liability = engine.calculateLiability()

        assertTrue("Deceptive entity should have higher liability",
            (liability["deceptive"]?.totalLiabilityPercent ?: 0f) >
            (liability["honest"]?.totalLiabilityPercent ?: 0f)
        )
    }

    @Test
    fun `test behavioral pattern detection`() {
        val engine = ContradictionEngine()
        val now = LocalDateTime.now()

        engine.registerEntity(
            Entity(id = "blamer", primaryName = "Blame Shifter")
        )

        // Add claims with blame shifting patterns
        engine.addClaim(
            Claim(
                id = "b1",
                entityId = "blamer",
                statement = "It's your fault this happened",
                timestamp = now.minusDays(5),
                sourceDocument = "msg1.txt",
                sourceType = Claim.SourceType.WHATSAPP
            )
        )

        engine.addClaim(
            Claim(
                id = "b2",
                entityId = "blamer",
                statement = "If you hadn't done that, we wouldn't be here",
                timestamp = now.minusDays(3),
                sourceDocument = "msg2.txt",
                sourceType = Claim.SourceType.WHATSAPP
            )
        )

        val patterns = engine.analyzeBehavior()

        val blamePatterns = patterns.filter {
            it.entityId == "blamer" &&
            it.patternType == BehavioralPattern.PatternType.BLAME_SHIFTING
        }

        assertTrue("Should detect blame shifting pattern", blamePatterns.isNotEmpty())
    }
}

/**
 * Unit tests for PDF Seal Engine
 */
class PdfSealEngineTest {

    @Test
    fun `test report generation`() {
        val engine = PdfSealEngine()

        val report = engine.generateReport(
            title = "Test Report",
            entities = mapOf("entity1" to Entity(id = "entity1", primaryName = "Test Entity")),
            timeline = emptyList(),
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityMatrix = emptyMap()
        )

        assertNotNull("Report should be generated", report)
        assertTrue("Report ID should start with VO-", report.id.startsWith("VO-"))
        assertTrue("SHA-512 hash should be 128 chars", report.sha512Hash.length == 128)
    }

    @Test
    fun `test report verification`() {
        val engine = PdfSealEngine()

        val report = engine.generateReport(
            title = "Test Report",
            entities = emptyMap(),
            timeline = emptyList(),
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityMatrix = emptyMap()
        )

        assertTrue("Report should verify correctly", engine.verifyReport(report))
    }
}

/**
 * Unit tests for NLP Utilities
 */
class NlpUtilsTest {

    @Test
    fun `test entity extraction from text`() {
        val text = "John Smith sent an email to Jane Doe about the project."
        val entities = NlpUtils.extractEntityNames(text)

        assertTrue("Should extract John Smith", entities.contains("John Smith"))
        assertTrue("Should extract Jane Doe", entities.contains("Jane Doe"))
    }

    @Test
    fun `test email extraction`() {
        val text = "Contact me at john@example.com for more information."
        val entities = NlpUtils.extractEntityNames(text)

        assertTrue("Should extract email", entities.contains("john@example.com"))
    }

    @Test
    fun `test relative date normalization`() {
        val today = LocalDate.now()

        val yesterdayResult = NlpUtils.normalizeDate("yesterday", today)
        assertNotNull("Should parse yesterday", yesterdayResult)
        assertEquals("Yesterday should be one day before today",
            today.minusDays(1),
            yesterdayResult?.toLocalDate()
        )

        val lastWeekResult = NlpUtils.normalizeDate("last week", today)
        assertNotNull("Should parse last week", lastWeekResult)
        assertEquals("Last week should be 7 days before today",
            today.minusDays(7),
            lastWeekResult?.toLocalDate()
        )
    }

    @Test
    fun `test claim extraction`() {
        val text = "I never agreed to that deal. He said he would pay by Friday. The deal fell through."
        val claims = NlpUtils.extractClaims(text)

        assertTrue("Should extract claims", claims.isNotEmpty())

        val denials = claims.filter { it.isDenial }
        assertTrue("Should detect denial", denials.isNotEmpty())
    }
}
