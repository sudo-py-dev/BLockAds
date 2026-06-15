package com.blockads.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.blockads.app.core.data.settings.SettingsRepository
import com.blockads.app.core.vpn.BlockAdsVpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsRepository.appSettings.first()
            if (!settings.autoStartOnBoot) return@launch
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) return@launch // requires user interaction — cannot auto-start
            val vpnIntent =
                Intent(context, BlockAdsVpnService::class.java)
                    .setAction(BlockAdsVpnService.ACTION_START)
            context.startForegroundService(vpnIntent)
        }
    }
}
