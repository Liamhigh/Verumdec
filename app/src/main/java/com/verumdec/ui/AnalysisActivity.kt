package com.verumdec.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityAnalysisBinding
import com.verumdec.core.leveler.Leveler
import com.verumdec.core.leveler.LevelerOutput
import kotlinx.coroutines.launch

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var engine: Leveler

    companion object {
        var currentCase: Case? = null
        var levelerOutput: LevelerOutput? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NEW: Initialize Leveler engine
        engine = Leveler(this)

        setupToolbar()
        displayResults()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun displayResults() {
        val case = currentCase ?: return

        // Display entities
        val entityAdapter = EntityAdapter(case.liabilityScores)
        binding.recyclerEntities.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = entityAdapter
        }
        entityAdapter.submitList(case.entities)

        // Display timeline
        val timelineAdapter = TimelineAdapter(case.entities)
        binding.recyclerTimeline.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = timelineAdapter
        }
        timelineAdapter.submitList(case.timeline.take(20)) // Limit display

        // Display contradictions
        if (case.contradictions.isEmpty()) {
            binding.textNoContradictions.visibility = View.VISIBLE
            binding.recyclerContradictions.visibility = View.GONE
        } else {
            binding.textNoContradictions.visibility = View.GONE
            val contradictionAdapter = ContradictionAdapter(case.entities)
            binding.recyclerContradictions.apply {
                layoutManager = LinearLayoutManager(this@AnalysisActivity)
                adapter = contradictionAdapter
            }
            contradictionAdapter.submitList(case.contradictions)
        }

        // Display liability scores
        val liabilityAdapter = LiabilityAdapter(case.entities)
        binding.recyclerLiability.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = liabilityAdapter
        }
        liabilityAdapter.submitList(case.liabilityScores.values.toList().sortedByDescending { it.overallScore })

        // Display narrative (now includes Leveler engine output)
        binding.textNarrative.text = case.narrative.ifEmpty { 
            "Narrative will be generated with the full report."
        }
    }

    private fun setupButtons() {
        binding.btnGenerateReport.setOnClickListener {
            generateReport()
        }
    }

    private fun generateReport() {
        val case = currentCase ?: return
        val output = levelerOutput

        binding.btnGenerateReport.isEnabled = false
        binding.btnGenerateReport.text = "Generating with Leveler..."

        lifecycleScope.launch {
            try {
                // Generate report using new Leveler engine output
                val file = LevelerReportGenerator(this@AnalysisActivity).generateReport(case, output)
                
                binding.btnGenerateReport.isEnabled = true
                binding.btnGenerateReport.text = getString(R.string.btn_generate_report)

                // Share the PDF
                val uri = FileProvider.getUriForFile(
                    this@AnalysisActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share Leveler Report"))

            } catch (e: Exception) {
                binding.btnGenerateReport.isEnabled = true
                binding.btnGenerateReport.text = getString(R.string.btn_generate_report)
                Toast.makeText(this@AnalysisActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
