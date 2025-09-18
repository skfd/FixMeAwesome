package com.surveyme.presentation.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.surveyme.databinding.FragmentHomeBinding
import com.surveyme.presentation.base.BaseFragment
import timber.log.Timber

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("HomeFragment onViewCreated")

        checkAndDisplayCrash()
    }

    private fun checkAndDisplayCrash() {
        val prefs = requireContext().getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)

        if (lastCrash != null) {
            Timber.e("Displaying crash: $lastCrash")

            binding.cardCrash.visibility = View.VISIBLE
            binding.textCrashInfo.text = lastCrash

            binding.buttonClearCrash.setOnClickListener {
                prefs.edit().remove("last_crash").apply()
                binding.cardCrash.visibility = View.GONE
                Timber.d("Crash log cleared")
            }
        } else {
            binding.cardCrash.visibility = View.GONE
        }
    }
}