package com.surveyme

import android.app.Application
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class SurveyMeApplication : Application() {

    companion object {
        lateinit var instance: SurveyMeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupCrashHandler()
        initializeTimber()
    }

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree())
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrashToFile(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logCrashToFile(throwable: Throwable) {
        try {
            val logFile = File(filesDir, "crash_log.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)
            val stackTrace = stringWriter.toString()

            FileWriter(logFile, true).use { writer ->
                writer.appendLine("\n=== CRASH at $timestamp ===")
                writer.appendLine("Message: ${throwable.message}")
                writer.appendLine("Stack trace:")
                writer.appendLine(stackTrace)
                writer.appendLine("=== END CRASH ===\n")
            }
        } catch (e: Exception) {
            Log.e("SurveyMeApp", "Failed to write crash log", e)
        }
    }

    fun getLogFile(): File {
        return File(filesDir, "app_log.txt")
    }

    fun getCrashLogFile(): File {
        return File(filesDir, "crash_log.txt")
    }

    inner class FileLoggingTree : Timber.Tree() {
        private val logFile = File(filesDir, "app_log.txt")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }

                // Keep file size manageable (max 1MB)
                if (logFile.length() > 1024 * 1024) {
                    logFile.writeText("") // Clear file if too large
                }

                val priorityStr = when (priority) {
                    Log.VERBOSE -> "V"
                    Log.DEBUG -> "D"
                    Log.INFO -> "I"
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> "?"
                }

                val timestamp = dateFormat.format(Date())
                FileWriter(logFile, true).use { writer ->
                    writer.appendLine("$timestamp $priorityStr/$tag: $message")
                    t?.let {
                        val stringWriter = StringWriter()
                        val printWriter = PrintWriter(stringWriter)
                        t.printStackTrace(printWriter)
                        writer.appendLine(stringWriter.toString())
                    }
                }
            } catch (e: Exception) {
                Log.e("FileLoggingTree", "Failed to write log", e)
            }
        }
    }
}