package com.verumdec.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
            textSeverity.text = violation.severity.name
            
            val (bgColor, textColor) = when (violation.severity) {
                ConstitutionViolationSeverity.CRITICAL -> Color.parseColor("#D32F2F") to Color.WHITE
                ConstitutionViolationSeverity.HIGH -> Color.parseColor("#F57C00") to Color.WHITE
                ConstitutionViolationSeverity.MEDIUM -> Color.parseColor("#FBC02D") to Color.BLACK
                ConstitutionViolationSeverity.LOW -> Color.parseColor("#388E3C") to Color.WHITE
            }
            
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
