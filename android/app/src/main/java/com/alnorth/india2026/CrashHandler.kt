package com.alnorth.india2026

import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Save crash info to preferences
        val prefs = context.getSharedPreferences("crash_data", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("crash_message", throwable.stackTraceToString())
            putLong("crash_time", System.currentTimeMillis())
            apply()
        }

        // Restart the app to show crash screen
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}
