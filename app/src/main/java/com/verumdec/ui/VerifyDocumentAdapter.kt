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
import com.google.android.material.button.MaterialButton
import com.verumdec.R
import com.verumdec.data.Evidence
import com.verumdec.data.EvidenceType

/**
 * Adapter for displaying documents in the verification list.
 */
class VerifyDocumentAdapter(
    private val onItemClick: (Evidence) -> Unit,
    private val onVerifyClick: (Evidence) -> Unit,
    private val onRemoveClick: (Evidence) -> Unit
) : ListAdapter<Evidence, VerifyDocumentAdapter.ViewHolder>(EvidenceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verify_document, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconDocument: ImageView = itemView.findViewById(R.id.iconDocument)
        private val textFileName: TextView = itemView.findViewById(R.id.textFileName)
        private val textFileType: TextView = itemView.findViewById(R.id.textFileType)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val iconStatus: ImageView = itemView.findViewById(R.id.iconStatus)
        private val btnVerify: MaterialButton = itemView.findViewById(R.id.btnVerify)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(evidence: Evidence) {
            textFileName.text = evidence.fileName
            textFileType.text = evidence.type.name

            val iconRes = when (evidence.type) {
                EvidenceType.PDF -> R.drawable.ic_pdf
                EvidenceType.IMAGE -> R.drawable.ic_document
                else -> R.drawable.ic_document
            }
            iconDocument.setImageResource(iconRes)

            if (evidence.processed) {
                textStatus.text = itemView.context.getString(R.string.status_verified)
                textStatus.setTextColor(itemView.context.getColor(R.color.success))
                iconStatus.setImageResource(R.drawable.ic_check)
                iconStatus.setColorFilter(itemView.context.getColor(R.color.success))
                btnVerify.visibility = View.GONE
            } else {
                textStatus.text = itemView.context.getString(R.string.status_pending)
                textStatus.setTextColor(itemView.context.getColor(R.color.warning))
                iconStatus.setImageResource(R.drawable.ic_pending)
                iconStatus.setColorFilter(itemView.context.getColor(R.color.warning))
                btnVerify.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { onItemClick(evidence) }
            btnVerify.setOnClickListener { onVerifyClick(evidence) }
            btnRemove.setOnClickListener { onRemoveClick(evidence) }
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
