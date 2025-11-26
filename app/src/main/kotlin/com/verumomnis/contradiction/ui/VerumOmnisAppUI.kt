package com.verumomnis.contradiction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verumomnis.contradiction.engine.*
import com.verumomnis.contradiction.pdf.ForensicReport
import com.verumomnis.contradiction.pdf.PdfSealEngine
import com.verumomnis.contradiction.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main Verum Omnis Application Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerumOmnisApp() {
    val navController = rememberNavController()
    val engine = remember { ContradictionEngine() }
    val pdfEngine = remember { PdfSealEngine() }

    var currentReport by remember { mutableStateOf<ForensicReport?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verum Omnis",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Evidence") },
                    label = { Text("Evidence") },
                    selected = currentRoute == "evidence",
                    onClick = { navController.navigate("evidence") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
                    label = { Text("Analysis") },
                    selected = currentRoute == "analysis",
                    onClick = { navController.navigate("analysis") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = "Report") },
                    label = { Text("Report") },
                    selected = currentRoute == "report",
                    onClick = { navController.navigate("report") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    onAnalyzeClick = {
                        // Demo analysis with sample data
                        runDemoAnalysis(engine)
                        val contradictions = engine.analyzeContradictions()
                        val patterns = engine.analyzeBehavior()
                        val liability = engine.calculateLiability()

                        currentReport = pdfEngine.generateReport(
                            title = "Demo Forensic Analysis",
                            entities = engine.getEntities(),
                            timeline = engine.getTimeline(),
                            contradictions = contradictions,
                            behavioralPatterns = patterns,
                            liabilityMatrix = liability
                        )

                        navController.navigate("analysis")
                    }
                )
            }
            composable("evidence") {
                EvidenceScreen()
            }
            composable("analysis") {
                AnalysisScreen(
                    engine = engine,
                    report = currentReport
                )
            }
            composable("report") {
                ReportScreen(report = currentReport)
            }
        }
    }
}

/**
 * Home Screen
 */
@Composable
fun HomeScreen(onAnalyzeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verum Omnis",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Contradiction Engine",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Forensic Truth Analysis\nOffline • Secure • Legal-Grade",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onAnalyzeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Run Demo Analysis")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { /* Import evidence */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Evidence")
        }
    }
}

/**
 * Evidence Screen
 */
@Composable
fun EvidenceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Evidence Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Drop files here or tap to import",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Supported: PDF, Images, WhatsApp exports, Emails",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Analysis Screen
 */
@Composable
fun AnalysisScreen(
    engine: ContradictionEngine,
    report: ForensicReport?
) {
    if (report == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Run analysis from Home to see results")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Analysis Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Entities",
                    value = report.entities.size.toString(),
                    icon = Icons.Default.Person
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Contradictions",
                    value = report.contradictions.size.toString(),
                    icon = Icons.Default.Warning
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Timeline",
                    value = report.timeline.size.toString(),
                    icon = Icons.Default.Timeline
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Patterns",
                    value = report.behavioralPatterns.size.toString(),
                    icon = Icons.Default.Psychology
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Contradictions Section
        item {
            Text(
                text = "Detected Contradictions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(report.contradictions) { contradiction ->
            ContradictionCard(contradiction)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Liability Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Liability Matrix",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(report.liabilityMatrix.sortedByDescending { it.totalLiabilityPercent }) { entry ->
            LiabilityCard(entry)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Report Screen
 */
@Composable
fun ReportScreen(report: ForensicReport?) {
    if (report == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No report generated yet")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Sealed Forensic Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Report ID: ${report.id}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Generated: ${report.generatedAt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SHA-512: ${report.sha512Hash.take(32)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = { /* Export PDF */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Sealed PDF")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { /* Share */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Report")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Narrative Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = report.narrativeContent.take(2000) + if (report.narrativeContent.length > 2000) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Summary Card Component
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Contradiction Card Component
 */
@Composable
fun ContradictionCard(contradiction: Contradiction) {
    val severityColor = when (contradiction.severity) {
        Contradiction.Severity.CRITICAL -> SeverityCritical
        Contradiction.Severity.HIGH -> SeverityHigh
        Contradiction.Severity.MEDIUM -> SeverityMedium
        Contradiction.Severity.LOW -> SeverityLow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = severityColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${contradiction.severity.name} - ${contradiction.type.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = contradiction.explanation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Liability Card Component
 */
@Composable
fun LiabilityCard(entry: LiabilityEntry) {
    val liabilityColor = when {
        entry.totalLiabilityPercent > 70 -> LiabilityHigh
        entry.totalLiabilityPercent > 40 -> LiabilityMedium
        else -> LiabilityLow
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.entityId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", entry.totalLiabilityPercent)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = liabilityColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { entry.totalLiabilityPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = liabilityColor,
            )
        }
    }
}

/**
 * Runs demo analysis with sample data
 */
private fun runDemoAnalysis(engine: ContradictionEngine) {
    engine.reset()

    // Register demo entities
    engine.registerEntity(
        Entity(
            id = "Marius",
            primaryName = "Marius",
            aliases = setOf("M", "Marius V"),
            email = "marius@example.com"
        )
    )

    engine.registerEntity(
        Entity(
            id = "Kevin",
            primaryName = "Kevin",
            aliases = setOf("K", "Kevin S"),
            email = "kevin@example.com"
        )
    )

    engine.registerEntity(
        Entity(
            id = "Liam",
            primaryName = "Liam",
            aliases = setOf("L"),
            email = "liam@example.com"
        )
    )

    // Add demo claims showing contradictions
    val now = LocalDateTime.now()

    engine.addClaim(
        Claim(
            id = "claim_1",
            entityId = "Marius",
            statement = "No deal ever existed between us",
            timestamp = now.minusDays(30),
            sourceDocument = "email_march_25.eml",
            sourceType = Claim.SourceType.EMAIL
        )
    )

    engine.addClaim(
        Claim(
            id = "claim_2",
            entityId = "Marius",
            statement = "The deal fell through due to circumstances",
            timestamp = now.minusDays(20),
            sourceDocument = "whatsapp_april_4.txt",
            sourceType = Claim.SourceType.WHATSAPP
        )
    )

    engine.addClaim(
        Claim(
            id = "claim_3",
            entityId = "Marius",
            statement = "I did receive the money but it was for something else",
            timestamp = now.minusDays(10),
            sourceDocument = "whatsapp_april_14.txt",
            sourceType = Claim.SourceType.WHATSAPP
        )
    )

    engine.addClaim(
        Claim(
            id = "claim_4",
            entityId = "Kevin",
            statement = "I was not involved in any financial transactions",
            timestamp = now.minusDays(25),
            sourceDocument = "email_march_30.eml",
            sourceType = Claim.SourceType.EMAIL
        )
    )

    engine.addClaim(
        Claim(
            id = "claim_5",
            entityId = "Liam",
            statement = "I sent the payment as agreed on the contract date",
            timestamp = now.minusDays(35),
            sourceDocument = "bank_statement.pdf",
            sourceType = Claim.SourceType.PDF
        )
    )

    engine.addClaim(
        Claim(
            id = "claim_6",
            entityId = "Liam",
            statement = "The payment was confirmed and documented",
            timestamp = now.minusDays(34),
            sourceDocument = "confirmation_email.eml",
            sourceType = Claim.SourceType.EMAIL
        )
    )
}
