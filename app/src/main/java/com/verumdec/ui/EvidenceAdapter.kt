package com.verumdec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verumdec.R
import com.verumdec.data.Evidence
import com.verumdec.data.EvidenceType

class EvidenceAdapter(
    private val onDeleteClick: (Evidence) -> Unit
) : ListAdapter<Evidence, EvidenceAdapter.ViewHolder>(EvidenceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evidence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconEvidence: ImageView = itemView.findViewById(R.id.iconEvidence)
        private val textFileName: TextView = itemView.findViewById(R.id.textFileName)
        private val textFileType: TextView = itemView.findViewById(R.id.textFileType)
        private val iconStatus: ImageView = itemView.findViewById(R.id.iconStatus)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(evidence: Evidence) {
            textFileName.text = evidence.fileName
            textFileType.text = evidence.type.name
            
            // Set icon based on type
            val iconRes = when (evidence.type) {
                EvidenceType.PDF -> R.drawable.ic_pdf
                EvidenceType.IMAGE -> R.drawable.ic_document
                else -> R.drawable.ic_document
            }
            iconEvidence.setImageResource(iconRes)
            
            // Set status icon
            iconStatus.setImageResource(
                if (evidence.processed) R.drawable.ic_check else R.drawable.ic_pending
            )
            
            btnDelete.setOnClickListener {
                onDeleteClick(evidence)
            }
        }
    }

    class EvidenceDiffCallback : DiffUtil.ItemCallback<Evidence>() {
        override fun areItemsTheSame(oldItem: Evidence, newItem: Evidence): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Evidence, newItem: Evidence): Boolean {
            return oldItem == newItem
        }
    }
}
