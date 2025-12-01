package com.verumdec.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.verumdec.R
import com.verumdec.data.Contradiction
import com.verumdec.data.ContradictionType
import com.verumdec.data.Severity
import com.verumdec.databinding.DialogConstitutionAlertBinding

/**
 * Dialog for displaying constitutional and legal alerts about detected contradictions.
 * Shows relevant legal implications based on the type and severity of contradictions found.
 */
class ConstitutionAlertDialog : DialogFragment() {

    private var _binding: DialogConstitutionAlertBinding? = null
    private val binding get() = _binding!!

    private var contradictions: List<Contradiction> = emptyList()
    private var onAcknowledgeListener: (() -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogConstitutionAlertBinding.inflate(layoutInflater)
        
        setupContent()
        
        return MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Verumdec_AlertDialog)
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog wider
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupContent() {
        // Determine the highest severity
        val highestSeverity = contradictions.maxOfOrNull { 
            when (it.severity) {
                Severity.CRITICAL -> 4
                Severity.HIGH -> 3
                Severity.MEDIUM -> 2
                Severity.LOW -> 1
            }
        } ?: 0

        // Set alert icon and color based on severity
        val (iconRes, colorRes, titleRes) = when (highestSeverity) {
            4 -> Triple(R.drawable.ic_shield, R.color.severity_critical, R.string.alert_critical)
            3 -> Triple(R.drawable.ic_shield, R.color.severity_high, R.string.alert_high)
            2 -> Triple(R.drawable.ic_shield, R.color.warning, R.string.alert_medium)
            else -> Triple(R.drawable.ic_shield, R.color.info, R.string.alert_low)
        }

        binding.iconAlert.setImageResource(iconRes)
        binding.iconAlert.setColorFilter(requireContext().getColor(colorRes))
        binding.textAlertTitle.text = getString(titleRes)
        binding.textAlertTitle.setTextColor(requireContext().getColor(colorRes))

        // Build alert message
        val alertMessage = buildAlertMessage()
        binding.textAlertMessage.text = alertMessage

        // Build legal implications
        val legalImplications = buildLegalImplications()
        binding.textLegalImplications.text = legalImplications

        // Constitutional references
        val constitutionalRefs = buildConstitutionalReferences()
        binding.textConstitutionalRefs.text = constitutionalRefs

        // Set up buttons
        binding.btnAcknowledge.setOnClickListener {
            onAcknowledgeListener?.invoke()
            dismiss()
        }

        binding.btnDismiss.setOnClickListener {
            onDismissListener?.invoke()
            dismiss()
        }

        // Summary stats
        binding.textContradictionCount.text = getString(
            R.string.alert_contradiction_count, 
            contradictions.size
        )
        
        val criticalCount = contradictions.count { it.severity == Severity.CRITICAL }
        binding.textCriticalCount.text = getString(
            R.string.alert_critical_count, 
            criticalCount
        )
    }

    private fun buildAlertMessage(): String {
        val criticalContradictions = contradictions.filter { it.severity == Severity.CRITICAL }
        val highContradictions = contradictions.filter { it.severity == Severity.HIGH }

        return buildString {
            if (criticalContradictions.isNotEmpty()) {
                appendLine(getString(R.string.alert_msg_critical))
                appendLine()
                criticalContradictions.take(3).forEach { c ->
                    appendLine("• ${c.description.take(100)}...")
                }
                appendLine()
            }

            if (highContradictions.isNotEmpty()) {
                appendLine(getString(R.string.alert_msg_high))
                appendLine()
                highContradictions.take(2).forEach { c ->
                    appendLine("• ${c.description.take(80)}...")
                }
            }

            if (isEmpty()) {
                append(getString(R.string.alert_msg_general))
            }
        }
    }

    private fun buildLegalImplications(): String {
        val implications = mutableSetOf<String>()

        contradictions.forEach { contradiction ->
            when (contradiction.type) {
                ContradictionType.DIRECT -> {
                    implications.add(getString(R.string.implication_direct))
                }
                ContradictionType.CROSS_DOCUMENT -> {
                    implications.add(getString(R.string.implication_cross_document))
                }
                ContradictionType.BEHAVIORAL -> {
                    implications.add(getString(R.string.implication_behavioral))
                }
                ContradictionType.MISSING_EVIDENCE -> {
                    implications.add(getString(R.string.implication_missing_evidence))
                }
                ContradictionType.TEMPORAL -> {
                    implications.add(getString(R.string.implication_temporal))
                }
                ContradictionType.THIRD_PARTY -> {
                    implications.add(getString(R.string.implication_third_party))
                }
            }
        }

        return implications.joinToString("\n\n")
    }

    private fun buildConstitutionalReferences(): String {
        val references = mutableListOf<String>()

        // Add relevant constitutional references based on contradiction types
        if (contradictions.any { it.severity == Severity.CRITICAL }) {
            references.add(getString(R.string.const_ref_fraud))
        }

        if (contradictions.any { it.type == ContradictionType.BEHAVIORAL }) {
            references.add(getString(R.string.const_ref_manipulation))
        }

        if (contradictions.any { it.type == ContradictionType.MISSING_EVIDENCE }) {
            references.add(getString(R.string.const_ref_discovery))
        }

        if (references.isEmpty()) {
            references.add(getString(R.string.const_ref_general))
        }

        return references.joinToString("\n\n")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class Builder(private val context: Context) {
        private var contradictions: List<Contradiction> = emptyList()
        private var onAcknowledgeListener: (() -> Unit)? = null
        private var onDismissListener: (() -> Unit)? = null

        fun setContradictions(contradictions: List<Contradiction>): Builder {
            this.contradictions = contradictions
            return this
        }

        fun setOnAcknowledgeListener(listener: () -> Unit): Builder {
            this.onAcknowledgeListener = listener
            return this
        }

        fun setOnDismissListener(listener: () -> Unit): Builder {
            this.onDismissListener = listener
            return this
        }

        fun build(): ConstitutionAlertDialog {
            return ConstitutionAlertDialog().apply {
                this.contradictions = this@Builder.contradictions
                this.onAcknowledgeListener = this@Builder.onAcknowledgeListener
                this.onDismissListener = this@Builder.onDismissListener
            }
        }

        fun show(fragmentManager: FragmentManager, tag: String = "constitution_alert") {
            build().show(fragmentManager, tag)
        }
    }

    companion object {
        fun builder(context: Context): Builder = Builder(context)
    }
}
