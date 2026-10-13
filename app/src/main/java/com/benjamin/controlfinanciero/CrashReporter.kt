package com.benjamin.controlfinanciero

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/** Saves uncaught crashes on-device; the report is shown on the next app launch. */
object CrashReporter {
    private const val REPORT_FILE = "crash-report.txt"
    private val installed = AtomicBoolean(false)

    fun install(context: Context) {
        if (!installed.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                File(appContext.filesDir, REPORT_FILE).writeText(CrashReportFormatter.format(error, timestamp))
            } catch (_: Throwable) {
                // Never let diagnostic persistence replace the original crash.
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun read(context: Context): String? = runCatching {
        File(context.applicationContext.filesDir, REPORT_FILE).takeIf(File::exists)?.readText()
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, REPORT_FILE).delete() }
    }
}
