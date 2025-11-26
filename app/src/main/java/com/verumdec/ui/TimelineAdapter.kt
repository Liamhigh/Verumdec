package com.verumdec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verumdec.R
import com.verumdec.data.Entity
import com.verumdec.data.TimelineEvent
import java.text.SimpleDateFormat
import java.util.*

class TimelineAdapter(
    private val entities: List<Entity>
) : ListAdapter<TimelineEvent, TimelineAdapter.ViewHolder>(TimelineDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textEventDate: TextView = itemView.findViewById(R.id.textEventDate)
        private val textEventType: TextView = itemView.findViewById(R.id.textEventType)
        private val textEventDescription: TextView = itemView.findViewById(R.id.textEventDescription)

        fun bind(event: TimelineEvent) {
            textEventDate.text = dateFormat.format(event.date)
            textEventType.text = event.eventType.name.replace("_", " ")
            textEventDescription.text = event.description.take(200)
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
