package com.blockads.app

import android.app.Application
import com.blockads.app.ui.crash.CrashActivity
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BlockAdsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val crashInfo =
                    buildString {
                        appendLine("Thread: ${thread.name}")
                        appendLine("Exception: ${throwable::class.qualifiedName}")
                        appendLine("Message: ${throwable.message}")
                        appendLine()
                        appendLine("Stack trace:")
                        throwable.stackTrace.forEach { appendLine("  at $it") }
                        throwable.cause?.let { cause ->
                            appendLine()
                            appendLine("Caused by: ${cause::class.qualifiedName}: ${cause.message}")
                            cause.stackTrace.forEach { appendLine("  at $it") }
                        }
                    }
                val intent = CrashActivity.createIntent(this, crashInfo)
                startActivity(intent)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
