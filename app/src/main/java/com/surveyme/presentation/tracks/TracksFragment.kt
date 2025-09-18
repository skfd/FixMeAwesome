package com.surveyme.presentation.tracks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surveyme.databinding.FragmentTracksBinding
import com.surveyme.presentation.base.BaseFragment
import timber.log.Timber

class TracksFragment : BaseFragment<FragmentTracksBinding>() {

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
        // TODO: Initialize RecyclerView for tracks list
        Timber.d("Tracks list setup placeholder")
    }
}