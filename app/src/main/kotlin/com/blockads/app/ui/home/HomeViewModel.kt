package com.blockads.app.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockads.app.core.data.settings.SettingsRepository
import com.blockads.app.core.vpn.BlockAdsVpnService
import com.blockads.app.domain.model.AppSettings
import com.blockads.app.domain.model.DnsStats
import com.blockads.app.domain.model.VpnState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.blockads.app.core.util.DnsSettingsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val application: Application,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val vpnState: StateFlow<VpnState> = BlockAdsVpnService.vpnState

        val stats: StateFlow<DnsStats> = BlockAdsVpnService.stats

        val settings: StateFlow<AppSettings?> =
            settingsRepository.appSettings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val isPrivateDnsStrict: StateFlow<Boolean> =
            flow {
                while (true) {
                    emit(DnsSettingsHelper.isPrivateDnsStrict(application))
                    delay(3000)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        fun startVpn(context: Context) {
            val intent =
                Intent(context, BlockAdsVpnService::class.java)
                    .setAction(BlockAdsVpnService.ACTION_START)
            context.startForegroundService(intent)
        }

        fun stopVpn(context: Context) {
            val intent =
                Intent(context, BlockAdsVpnService::class.java)
                    .setAction(BlockAdsVpnService.ACTION_STOP)
            context.startService(intent)
        }

        fun pauseVpn(
            context: Context,
            durationMs: Long,
        ) {
            val intent =
                Intent(context, BlockAdsVpnService::class.java)
                    .setAction(BlockAdsVpnService.ACTION_PAUSE)
                    .putExtra(BlockAdsVpnService.EXTRA_PAUSE_DURATION_MS, durationMs)
            context.startService(intent)
        }

        fun resumeVpn(context: Context) {
            val intent =
                Intent(context, BlockAdsVpnService::class.java)
                    .setAction(BlockAdsVpnService.ACTION_RESUME)
            context.startService(intent)
        }
    }
