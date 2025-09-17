package com.surveyme.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        Timber.d("${this::class.simpleName} onCreateView")
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("${this::class.simpleName} onDestroyView")
    }

    protected fun showError(message: String) {
        Timber.e(message)
        // TODO: Implement error display (snackbar/toast)
    }

    protected fun showLoading(show: Boolean) {
        // TODO: Implement loading state
        Timber.d("${this::class.simpleName} showLoading: $show")
    }
}