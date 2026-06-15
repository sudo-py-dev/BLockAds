package com.blockads.app.core.util

import android.content.Context
import android.provider.Settings

object DnsSettingsHelper {
    fun isPrivateDnsStrict(context: Context): Boolean {
        return try {
            val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
            mode == "hostname"
        } catch (e: Exception) {
            false
        }
    }
}
