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
import com.verumdec.data.Contradiction
import com.verumdec.data.Entity
import com.verumdec.data.Severity

class ContradictionAdapter(
    private val entities: List<Entity>
) : ListAdapter<Contradiction, ContradictionAdapter.ViewHolder>(ContradictionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contradiction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSeverity: TextView = itemView.findViewById(R.id.textSeverity)
        private val textContradictionType: TextView = itemView.findViewById(R.id.textContradictionType)
        private val textEntityName: TextView = itemView.findViewById(R.id.textEntityName)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val textLegalImplication: TextView = itemView.findViewById(R.id.textLegalImplication)

        fun bind(contradiction: Contradiction) {
            textSeverity.text = contradiction.severity.name
            
            val (bgColor, textColor) = when (contradiction.severity) {
                Severity.CRITICAL -> Color.parseColor("#D32F2F") to Color.WHITE
                Severity.HIGH -> Color.parseColor("#F57C00") to Color.WHITE
                Severity.MEDIUM -> Color.parseColor("#FBC02D") to Color.BLACK
                Severity.LOW -> Color.parseColor("#388E3C") to Color.WHITE
            }
            
            val background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 8f
            }
            textSeverity.background = background
            textSeverity.setTextColor(textColor)
            
            textContradictionType.text = contradiction.type.name.replace("_", " ")
            
            val entity = entities.find { it.id == contradiction.entityId }
            textEntityName.text = entity?.primaryName ?: "Unknown"
            
            textDescription.text = contradiction.description
            textLegalImplication.text = contradiction.legalImplication
        }
    }

    class ContradictionDiffCallback : DiffUtil.ItemCallback<Contradiction>() {
        override fun areItemsTheSame(oldItem: Contradiction, newItem: Contradiction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contradiction, newItem: Contradiction): Boolean {
            return oldItem == newItem
        }
    }
}
