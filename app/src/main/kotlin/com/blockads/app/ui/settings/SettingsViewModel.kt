package com.blockads.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockads.app.core.data.blocklist.BlocklistRepository
import com.blockads.app.core.data.blocklist.BlocklistSource
import com.blockads.app.core.data.settings.SettingsRepository
import com.blockads.app.domain.model.AppSettings
import com.blockads.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val blocklistRepository: BlocklistRepository,
    ) : ViewModel() {
        val settings: StateFlow<AppSettings> =
            settingsRepository.appSettings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.default())

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        private val _refreshError = MutableStateFlow<String?>(null)
        val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

        fun setTheme(mode: ThemeMode) {
            viewModelScope.launch { settingsRepository.setThemeMode(mode) }
        }

        fun setLanguage(tag: String) {
            viewModelScope.launch { settingsRepository.setLanguageTag(tag) }
        }

        fun setBlocklistSource(source: BlocklistSource) {
            viewModelScope.launch {
                settingsRepository.setBlocklistSource(source)
                if (source.upstreamDns != null) {
                    settingsRepository.setDns(source.upstreamDns!!.first, source.upstreamDns!!.second)
                }
            }
        }

        fun setCustomUrl(url: String) {
            viewModelScope.launch { settingsRepository.setCustomUrl(url) }
        }

        fun setDns(
            primary: String,
            secondary: String,
        ) {
            viewModelScope.launch { settingsRepository.setDns(primary, secondary) }
        }

        fun setAutoStart(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.setAutoStart(enabled) }
        }

        fun setNotificationStats(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.setNotificationStats(enabled) }
        }

        fun refreshBlocklist() {
            viewModelScope.launch {
                _isRefreshing.value = true
                _refreshError.value = null
                val source = settings.value.blocklistSource
                val result = blocklistRepository.refresh(source)
                if (result.isSuccess) {
                    settingsRepository.setBlocklistLastUpdated(System.currentTimeMillis())
                } else {
                    _refreshError.value = result.exceptionOrNull()?.message
                }
                _isRefreshing.value = false
            }
        }

        fun clearRefreshError() {
            _refreshError.value = null
        }
    }
