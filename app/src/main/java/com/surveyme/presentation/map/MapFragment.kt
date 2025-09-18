package com.surveyme.presentation.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surveyme.databinding.FragmentMapBinding
import com.surveyme.presentation.base.BaseFragment
import timber.log.Timber

class MapFragment : BaseFragment<FragmentMapBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapBinding {
        return FragmentMapBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("MapFragment onViewCreated")
        setupMap()
    }

    private fun setupMap() {
        // TODO: Initialize map view
        Timber.d("Map setup placeholder")
    }
}