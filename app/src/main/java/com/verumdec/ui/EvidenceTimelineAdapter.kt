package com.verumdec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verumdec.R
import com.verumdec.data.Entity
import com.verumdec.data.EventType
import com.verumdec.data.Significance
import com.verumdec.data.TimelineEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying timeline events with visual indicators.
 */
class EvidenceTimelineAdapter(
    private val entities: List<Entity>,
    private val onEventClick: (TimelineEvent) -> Unit
) : ListAdapter<TimelineEvent, EvidenceTimelineAdapter.ViewHolder>(TimelineDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evidence_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isFirst = position == 0
        val isLast = position == itemCount - 1
        holder.bind(getItem(position), isFirst, isLast)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewTopLine: View = itemView.findViewById(R.id.viewTopLine)
        private val viewBottomLine: View = itemView.findViewById(R.id.viewBottomLine)
        private val viewTimelineNode: View = itemView.findViewById(R.id.viewTimelineNode)
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textEventType: TextView = itemView.findViewById(R.id.textEventType)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val textEntities: TextView = itemView.findViewById(R.id.textEntities)
        private val textSignificance: TextView = itemView.findViewById(R.id.textSignificance)

        fun bind(event: TimelineEvent, isFirst: Boolean, isLast: Boolean) {
            val context = itemView.context

            // Timeline line visibility
            viewTopLine.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
            viewBottomLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

            // Node color based on significance
            val nodeColor = when (event.significance) {
                Significance.CRITICAL -> R.color.severity_critical
                Significance.HIGH -> R.color.severity_high
                Significance.NORMAL -> R.color.primary
                Significance.LOW -> R.color.text_secondary
            }
            viewTimelineNode.background.setTint(ContextCompat.getColor(context, nodeColor))

            // Date and time
            textDate.text = dateFormat.format(event.date)
            textTime.text = timeFormat.format(event.date)

            // Event type with icon indicator
            val typeIcon = when (event.eventType) {
                EventType.COMMUNICATION -> "💬"
                EventType.PAYMENT -> "💰"
                EventType.PROMISE -> "🤝"
                EventType.DOCUMENT -> "📄"
                EventType.CONTRADICTION -> "⚠️"
                EventType.ADMISSION -> "✅"
                EventType.DENIAL -> "❌"
                EventType.BEHAVIOR_CHANGE -> "🔄"
                EventType.OTHER -> "📌"
            }
            textEventType.text = "$typeIcon ${event.eventType.name.replace("_", " ")}"

            // Description
            textDescription.text = event.description.take(150).let {
                if (event.description.length > 150) "$it..." else it
            }

            // Entities involved
            val involvedEntities = event.entityIds.mapNotNull { id ->
                entities.find { it.id == id }?.primaryName
            }
            textEntities.text = if (involvedEntities.isNotEmpty()) {
                involvedEntities.joinToString(", ")
            } else {
                context.getString(R.string.no_entities_involved)
            }

            // Significance indicator
            textSignificance.text = event.significance.name
            val significanceColor = when (event.significance) {
                Significance.CRITICAL -> R.color.severity_critical
                Significance.HIGH -> R.color.severity_high
                Significance.NORMAL -> R.color.text_secondary
                Significance.LOW -> R.color.text_hint
            }
            textSignificance.setTextColor(ContextCompat.getColor(context, significanceColor))

            itemView.setOnClickListener { onEventClick(event) }
        }
    }

    class TimelineDiffCallback : DiffUtil.ItemCallback<TimelineEvent>() {
        override fun areItemsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
            return oldItem == newItem
        }
    }
}
