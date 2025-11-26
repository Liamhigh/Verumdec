package com.verumdec.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verumdec.R
import com.verumdec.data.Entity
import com.verumdec.data.LiabilityScore

class LiabilityAdapter(
    private val entities: List<Entity>
) : ListAdapter<LiabilityScore, LiabilityAdapter.ViewHolder>(LiabilityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textEntityName: TextView = itemView.findViewById(R.id.textEntityName)
        private val textLiabilityScore: TextView = itemView.findViewById(R.id.textLiabilityScore)
        private val textEntityDetails: TextView = itemView.findViewById(R.id.textEntityDetails)

        fun bind(score: LiabilityScore) {
            val entity = entities.find { it.id == score.entityId }
            textEntityName.text = entity?.primaryName ?: "Unknown"
            
            textLiabilityScore.text = String.format("%.0f%%", score.overallScore)
            textLiabilityScore.setTextColor(
                when {
                    score.overallScore >= 70 -> Color.parseColor("#D32F2F")
                    score.overallScore >= 40 -> Color.parseColor("#F57C00")
                    else -> Color.parseColor("#388E3C")
                }
            )
            
            val breakdown = score.breakdown
            val details = mutableListOf<String>()
            if (breakdown.totalContradictions > 0) {
                details.add("${breakdown.totalContradictions} contradictions")
            }
            if (breakdown.criticalContradictions > 0) {
                details.add("${breakdown.criticalContradictions} critical")
            }
            if (breakdown.behavioralFlags.isNotEmpty()) {
                details.add("${breakdown.behavioralFlags.size} behavioral flags")
            }
            
            textEntityDetails.text = if (details.isNotEmpty()) {
                details.joinToString(" • ")
            } else {
                "No significant issues detected"
            }
        }
    }

    class LiabilityDiffCallback : DiffUtil.ItemCallback<LiabilityScore>() {
        override fun areItemsTheSame(oldItem: LiabilityScore, newItem: LiabilityScore): Boolean {
            return oldItem.entityId == newItem.entityId
        }

        override fun areContentsTheSame(oldItem: LiabilityScore, newItem: LiabilityScore): Boolean {
            return oldItem == newItem
        }
    }
}
