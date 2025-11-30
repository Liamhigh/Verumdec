package com.verumdec.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityAnalysisBinding
import com.verumdec.engine.ContradictionEngine
import com.verumdec.viewmodel.ReportGenerationViewModel
import kotlinx.coroutines.launch

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var engine: ContradictionEngine
    private lateinit var reportViewModel: ReportGenerationViewModel

    companion object {
        var currentCase: Case? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = ContradictionEngine(this)
        reportViewModel = ViewModelProvider(this)[ReportGenerationViewModel::class.java]

        setupToolbar()
        displayResults()
        setupButtons()
        observeViewModel()
        
        // Show constitution alert if critical contradictions exist
        checkForCriticalContradictions()
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

        // Display narrative
        binding.textNarrative.text = case.narrative.ifEmpty { 
            "Narrative will be generated with the full report."
        }
    }

    private fun setupButtons() {
        binding.btnGenerateReport.setOnClickListener {
            generateReport()
        }
        
        binding.btnViewFullTimeline.setOnClickListener {
            viewFullTimeline()
        }
    }

    private fun observeViewModel() {
        reportViewModel.generationState.observe(this) { state ->
            when (state) {
                is ReportGenerationViewModel.GenerationState.Generating -> {
                    binding.btnGenerateReport.isEnabled = false
                    binding.btnGenerateReport.text = state.message
                }
                is ReportGenerationViewModel.GenerationState.Complete -> {
                    binding.btnGenerateReport.isEnabled = true
                    binding.btnGenerateReport.text = getString(R.string.btn_generate_report)
                    
                    // Open report viewer
                    NavigationHelper.navigateToReportViewer(this, state.file)
                }
                is ReportGenerationViewModel.GenerationState.Error -> {
                    binding.btnGenerateReport.isEnabled = true
                    binding.btnGenerateReport.text = getString(R.string.btn_generate_report)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnGenerateReport.isEnabled = true
                    binding.btnGenerateReport.text = getString(R.string.btn_generate_report)
                }
            }
        }
    }

    private fun generateReport() {
        val case = currentCase ?: return
        reportViewModel.generateReport(case)
    }
    
    private fun checkForCriticalContradictions() {
        val case = currentCase ?: return
        val criticalCount = case.contradictions.count { it.severity == Severity.CRITICAL }
        
        if (criticalCount > 0) {
            NavigationHelper.showConstitutionAlert(
                context = this,
                fragmentManager = supportFragmentManager,
                case = case,
                onAcknowledge = {
                    // User acknowledged the alert
                },
                onDismiss = {
                    // User dismissed the alert
                }
            )
        }
    }

    /**
     * Navigate to the full timeline view.
     */
    fun viewFullTimeline() {
        val case = currentCase ?: return
        NavigationHelper.navigateToTimeline(this, case)
    }
}
