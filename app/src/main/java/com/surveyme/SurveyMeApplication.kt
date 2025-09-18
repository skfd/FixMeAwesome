package com.surveyme

import android.app.Application
import android.content.Context
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
        initializeOSMDroid()
    }

    private fun initializeOSMDroid() {
        try {
            // Initialize OSMDroid configuration
            val config = org.osmdroid.config.Configuration.getInstance()
            config.load(this, android.preference.PreferenceManager.getDefaultSharedPreferences(this))
            // Set a user agent to avoid getting banned from tile servers
            config.userAgentValue = packageName
            Timber.d("OSMDroid initialized with user agent: $packageName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize OSMDroid")
        }
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
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)
            val stackTrace = stringWriter.toString()

            // Save to file
            val logFile = File(filesDir, "crash_log.txt")
            FileWriter(logFile, true).use { writer ->
                writer.appendLine("\n=== CRASH at $timestamp ===")
                writer.appendLine("Message: ${throwable.message}")
                writer.appendLine("Stack trace:")
                writer.appendLine(stackTrace)
                writer.appendLine("=== END CRASH ===\n")
            }

            // ALSO save to SharedPreferences for immediate access
            val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
            val crashInfo = buildString {
                appendLine("CRASH at $timestamp")
                appendLine("Error: ${throwable.message}")
                appendLine("")
                appendLine("Location: ${throwable.stackTrace.firstOrNull()}")
                appendLine("")
                appendLine("First 5 stack frames:")
                throwable.stackTrace.take(5).forEach { frame ->
                    appendLine("  at $frame")
                }
            }
            prefs.edit().putString("last_crash", crashInfo).apply()

            Log.e("SurveyMeApp", "CRASH SAVED TO PREFS: $crashInfo")
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