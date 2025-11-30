package com.verumdec.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityEvidenceTimelineBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for displaying the evidence timeline.
 * Shows a chronological view of all events discovered during analysis.
 */
class EvidenceTimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvidenceTimelineBinding
    private lateinit var timelineAdapter: EvidenceTimelineAdapter

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEvidenceTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        loadTimeline()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_timeline)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        timelineAdapter = EvidenceTimelineAdapter(
            entities = currentCase?.entities ?: emptyList(),
            onEventClick = { event -> showEventDetails(event) }
        )

        binding.recyclerTimeline.apply {
            layoutManager = LinearLayoutManager(this@EvidenceTimelineActivity)
            adapter = timelineAdapter
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener { filterEvents(null) }
        binding.chipCritical.setOnClickListener { filterEvents(Significance.CRITICAL) }
        binding.chipHigh.setOnClickListener { filterEvents(Significance.HIGH) }
        binding.chipContradictions.setOnClickListener { filterByType(EventType.CONTRADICTION) }
        binding.chipPayments.setOnClickListener { filterByType(EventType.PAYMENT) }
    }

    private fun loadTimeline() {
        val case = currentCase
        if (case == null) {
            binding.textEmptyState.visibility = View.VISIBLE
            binding.recyclerTimeline.visibility = View.GONE
            return
        }

        val timeline = case.timeline.sortedBy { it.date }
        
        if (timeline.isEmpty()) {
            binding.textEmptyState.visibility = View.VISIBLE
            binding.recyclerTimeline.visibility = View.GONE
        } else {
            binding.textEmptyState.visibility = View.GONE
            binding.recyclerTimeline.visibility = View.VISIBLE
            timelineAdapter.submitList(timeline)
            updateSummary(timeline)
        }
    }

    private fun filterEvents(significance: Significance?) {
        val case = currentCase ?: return
        val filtered = if (significance == null) {
            case.timeline
        } else {
            case.timeline.filter { it.significance == significance }
        }
        timelineAdapter.submitList(filtered.sortedBy { it.date })
    }

    private fun filterByType(type: EventType) {
        val case = currentCase ?: return
        val filtered = case.timeline.filter { it.eventType == type }
        timelineAdapter.submitList(filtered.sortedBy { it.date })
    }

    private fun showEventDetails(event: TimelineEvent) {
        val case = currentCase ?: return
        val entities = event.entityIds.mapNotNull { id ->
            case.entities.find { it.id == id }
        }

        val entityNames = entities.joinToString(", ") { it.primaryName }
        val evidence = case.evidence.find { it.id == event.sourceEvidenceId }

        val message = buildString {
            appendLine("Date: ${dateFormat.format(event.date)}")
            appendLine("Type: ${event.eventType.name.replace("_", " ")}")
            appendLine("Significance: ${event.significance.name}")
            appendLine()
            appendLine("Description:")
            appendLine(event.description)
            if (entityNames.isNotEmpty()) {
                appendLine()
                appendLine("Entities Involved: $entityNames")
            }
            if (evidence != null) {
                appendLine()
                appendLine("Source: ${evidence.fileName}")
            }
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_event_details))
            .setMessage(message)
            .setPositiveButton(R.string.btn_ok, null)
            .show()
    }

    private fun updateSummary(timeline: List<TimelineEvent>) {
        if (timeline.isEmpty()) return

        val earliest = timeline.minByOrNull { it.date }
        val latest = timeline.maxByOrNull { it.date }
        val critical = timeline.count { it.significance == Significance.CRITICAL }
        val contradictions = timeline.count { it.eventType == EventType.CONTRADICTION }

        binding.textTimeRange.text = if (earliest != null && latest != null) {
            getString(
                R.string.timeline_range,
                dateFormat.format(earliest.date),
                dateFormat.format(latest.date)
            )
        } else ""

        binding.textEventCount.text = getString(R.string.timeline_event_count, timeline.size)
        binding.textCriticalCount.text = getString(R.string.timeline_critical_count, critical)
        binding.textContradictionCount.text = getString(
            R.string.timeline_contradiction_count, 
            contradictions
        )
    }

    companion object {
        var currentCase: Case? = null

        fun newIntent(context: Context, case: Case): Intent {
            currentCase = case
            return Intent(context, EvidenceTimelineActivity::class.java)
        }
    }
}
