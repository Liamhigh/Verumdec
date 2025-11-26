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

class EntityAdapter(
    private val liabilityScores: Map<String, LiabilityScore>
) : ListAdapter<Entity, EntityAdapter.ViewHolder>(EntityDiffCallback()) {

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

        fun bind(entity: Entity) {
            textEntityName.text = entity.primaryName
            
            val score = liabilityScores[entity.id]
            if (score != null) {
                val scoreValue = score.overallScore
                textLiabilityScore.text = String.format("%.0f%%", scoreValue)
                textLiabilityScore.setTextColor(
                    when {
                        scoreValue >= 70 -> Color.parseColor("#D32F2F")
                        scoreValue >= 40 -> Color.parseColor("#F57C00")
                        else -> Color.parseColor("#388E3C")
                    }
                )
            } else {
                textLiabilityScore.text = "N/A"
            }
            
            val details = mutableListOf<String>()
            if (entity.emails.isNotEmpty()) {
                details.add(entity.emails.first())
            }
            details.add("${entity.mentions} mentions")
            if (entity.aliases.isNotEmpty()) {
                details.add("Also: ${entity.aliases.take(2).joinToString(", ")}")
            }
            
            textEntityDetails.text = details.joinToString(" • ")
        }
    }

    class EntityDiffCallback : DiffUtil.ItemCallback<Entity>() {
        override fun areItemsTheSame(oldItem: Entity, newItem: Entity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Entity, newItem: Entity): Boolean {
            return oldItem == newItem
        }
    }
}
