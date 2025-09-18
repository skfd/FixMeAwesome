package com.surveyme.presentation.debug

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.surveyme.R
import java.io.File

class LogViewerDialog : DialogFragment() {

    companion object {
        private const val ARG_LOG_FILE = "log_file"
        private const val ARG_TITLE = "title"

        fun newInstance(logFile: File, title: String): LogViewerDialog {
            return LogViewerDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_LOG_FILE, logFile)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val logFile = arguments?.getSerializable(ARG_LOG_FILE) as? File
        val title = arguments?.getString(ARG_TITLE) ?: getString(R.string.logs_title)

        val logContent = if (logFile?.exists() == true) {
            try {
                logFile.readText().takeIf { it.isNotBlank() }
                    ?: getString(R.string.no_logs)
            } catch (e: Exception) {
                "Error reading logs: ${e.message}"
            }
        } else {
            getString(R.string.no_logs)
        }

        val textView = TextView(requireContext()).apply {
            text = logContent
            setPadding(48, 24, 48, 24)
            setTextIsSelectable(true)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(textView)
            .setPositiveButton("Close") { _, _ -> dismiss() }
            .setNeutralButton("Copy") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Log", logContent)
                clipboard.setPrimaryClip(clip)
            }
            .create()
    }
}