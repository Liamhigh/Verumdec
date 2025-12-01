package com.verumdec.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verumdec.R
import com.verumdec.data.ConstitutionViolation
import com.verumdec.data.ConstitutionViolationSeverity

/**
 * Adapter for displaying constitution violations in a RecyclerView.
 */
class ViolationAdapter : ListAdapter<ConstitutionViolation, ViolationAdapter.ViewHolder>(ViolationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_violation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSeverity: TextView = itemView.findViewById(R.id.textViolationSeverity)
        private val textViolationType: TextView = itemView.findViewById(R.id.textViolationType)
        private val textRuleName: TextView = itemView.findViewById(R.id.textRuleName)
        private val textDescription: TextView = itemView.findViewById(R.id.textViolationDescription)
        private val textDetails: TextView = itemView.findViewById(R.id.textViolationDetails)

        fun bind(violation: ConstitutionViolation) {
            val context = itemView.context
            textSeverity.text = violation.severity.name
            
            val (bgColorRes, textColor) = when (violation.severity) {
                ConstitutionViolationSeverity.CRITICAL -> R.color.severity_critical to Color.WHITE
                ConstitutionViolationSeverity.HIGH -> R.color.severity_high to Color.WHITE
                ConstitutionViolationSeverity.MEDIUM -> R.color.severity_medium to Color.BLACK
                ConstitutionViolationSeverity.LOW -> R.color.severity_low to Color.WHITE
            }
            
            val bgColor = ContextCompat.getColor(context, bgColorRes)
            val background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 8f
            }
            textSeverity.background = background
            textSeverity.setTextColor(textColor)
            
            textViolationType.text = violation.checkType.name.replace("_", " ")
            textRuleName.text = violation.ruleName
            textDescription.text = violation.description
            
            if (violation.details.isNotBlank()) {
                textDetails.visibility = View.VISIBLE
                textDetails.text = violation.details
            } else {
                textDetails.visibility = View.GONE
            }
        }
    }

    class ViolationDiffCallback : DiffUtil.ItemCallback<ConstitutionViolation>() {
        override fun areItemsTheSame(oldItem: ConstitutionViolation, newItem: ConstitutionViolation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConstitutionViolation, newItem: ConstitutionViolation): Boolean {
            return oldItem == newItem
        }
    }
}
