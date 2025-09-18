package com.surveyme.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.surveyme.BuildConfig
import com.surveyme.R
import com.surveyme.SurveyMeApplication
import com.surveyme.core.PreferencesManager
import com.surveyme.databinding.FragmentSettingsBinding
import com.surveyme.presentation.base.BaseFragment
import com.surveyme.presentation.debug.LogViewerDialog
import timber.log.Timber

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private lateinit var preferencesManager: PreferencesManager

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("SettingsFragment onViewCreated")

        preferencesManager = PreferencesManager(requireContext())
        setupSettings()
        setupDebugSection()
    }

    private fun setupSettings() {
        with(binding) {
            // Set current values
            switchDarkMode.isChecked = preferencesManager.isDarkMode
            switchNotifications.isChecked = preferencesManager.areNotificationsEnabled
            textMapStyle.text = preferencesManager.mapStyle.capitalize()
            textVersion.text = BuildConfig.VERSION_NAME

            // Set listeners
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.isDarkMode = isChecked
            }

            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.areNotificationsEnabled = isChecked
            }
        }
    }

    private fun setupDebugSection() {
        if (BuildConfig.DEBUG) {
            with(binding) {
                // Show debug section in debug builds
                textDebugTitle.visibility = View.VISIBLE
                cardDebug.visibility = View.VISIBLE

                buttonViewLogs.setOnClickListener {
                    val logFile = SurveyMeApplication.instance.getLogFile()
                    LogViewerDialog.newInstance(logFile, getString(R.string.logs_title))
                        .show(parentFragmentManager, "logs")
                }

                buttonViewCrashLogs.setOnClickListener {
                    val crashLogFile = SurveyMeApplication.instance.getCrashLogFile()
                    LogViewerDialog.newInstance(crashLogFile, getString(R.string.crash_logs_title))
                        .show(parentFragmentManager, "crash_logs")
                }

                buttonClearLogs.setOnClickListener {
                    try {
                        SurveyMeApplication.instance.getLogFile().writeText("")
                        SurveyMeApplication.instance.getCrashLogFile().writeText("")
                        Toast.makeText(requireContext(), getString(R.string.logs_cleared), Toast.LENGTH_SHORT).show()
                        Timber.d("Logs cleared by user")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to clear logs")
                        Toast.makeText(requireContext(), "Failed to clear logs", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}