package com.adsblock.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.adsblock.vpn.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val settingsRepository = SettingsRepository(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val autoConnect = settingsRepository.autoConnectOnBoot.first()
                if (autoConnect) {
                    val vpnIntent = android.net.VpnService.prepare(context)
                    // If vpnIntent is null, it means the user has already granted VPN permissions
                    if (vpnIntent == null) {
                        val startIntent = Intent(context, AdsBlockVpnService::class.java).apply {
                            action = AdsBlockVpnService.ACTION_START
                        }
                        ContextCompat.startForegroundService(context, startIntent)
                    }
                }
            }
        }
    }
}
