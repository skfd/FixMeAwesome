package com.surveyme.presentation.tracks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.surveyme.data.database.TrackEntity
import com.surveyme.databinding.ItemTrackBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TracksAdapter(
    private val onTrackClick: (TrackEntity) -> Unit,
    private val onTrackLongClick: (TrackEntity) -> Unit
) : ListAdapter<TrackEntity, TracksAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackViewHolder(
        private val binding: ItemTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTrackClick(getItem(position))
                }
            }
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTrackLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
        }

        fun bind(track: TrackEntity) {
            binding.textTrackName.text = track.name
            
            // Format Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            binding.textTrackDate.text = dateFormat.format(Date(track.startTime))
            
            // Format Distance
            val distanceKm = track.totalDistance / 1000f
            binding.textTrackDistance.text = String.format(Locale.getDefault(), "%.2f km", distanceKm)
            
            // Format Duration
            val durationMinutes = track.duration / (1000 * 60)
            binding.textTrackDuration.text = "$durationMinutes min"
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<TrackEntity>() {
        override fun areItemsTheSame(oldItem: TrackEntity, newItem: TrackEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrackEntity, newItem: TrackEntity): Boolean {
            return oldItem == newItem
        }
    }
}
