package com.adsblock.vpn

import android.app.Application
import android.content.Intent
import com.adsblock.vpn.data.DnsProviders
import com.adsblock.vpn.ui.CrashActivity
import com.adsblock.vpn.util.Logger

class AdsBlockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DnsProviders.init(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Logger.e("AdsBlockApp", "Uncaught exception", exception)

            val intent =
                Intent(this, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            startActivity(intent)

            // Allow the system to kill the process after we started our CrashActivity
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }
}
