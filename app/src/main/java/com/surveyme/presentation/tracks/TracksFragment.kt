package com.surveyme.presentation.tracks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surveyme.databinding.FragmentTracksBinding
import androidx.lifecycle.lifecycleScope
import com.surveyme.data.TrackManager
import com.surveyme.presentation.base.BaseFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class TracksFragment : BaseFragment<FragmentTracksBinding>() {

    private lateinit var tracksAdapter: TracksAdapter

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTracksBinding {
        return FragmentTracksBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("TracksFragment onViewCreated")
        setupTracksList()
    }

    private fun setupTracksList() {
        tracksAdapter = TracksAdapter(
            onTrackClick = { track ->
                // TODO: Navigate to map view later (we might need a view action or a new fragment)
                Timber.d("Track clicked: ${track.name}")
            },
            onTrackLongClick = { track ->
                showDeleteConfirmationDialog(track)
            }
        )
        binding.recyclerViewTracks.adapter = tracksAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            val trackRepository = TrackManager.getRepository(requireContext())
            trackRepository.getAllTracksFlow().collectLatest { tracks ->
                Timber.d("Loaded ${tracks.size} tracks from database")
                tracksAdapter.submitList(tracks)
                updateEmptyState(tracks.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewTracks.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewTracks.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(track: com.surveyme.data.database.TrackEntity) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Track")
            .setMessage("Are you sure you want to delete '${track.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val trackRepository = TrackManager.getRepository(requireContext())
                        trackRepository.deleteTrack(track)
                        Timber.d("Track deleted successfully")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to delete track")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}