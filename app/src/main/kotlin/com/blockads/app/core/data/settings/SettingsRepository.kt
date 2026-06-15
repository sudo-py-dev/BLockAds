package com.blockads.app.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.blockads.app.core.data.blocklist.BlocklistSource
import com.blockads.app.domain.model.AppSettings
import com.blockads.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val appSettings: Flow<AppSettings> =
            dataStore.data.map { prefs ->
                val sourceKey = prefs[SettingsKeys.blocklistSourceKey] ?: "bundled"
                val customUrl = prefs[SettingsKeys.customUrlKey] ?: ""
                AppSettings(
                    blocklistSource = BlocklistSource.fromKey(sourceKey, customUrl),
                    customBlocklistUrl = customUrl,
                    dnsPrimary = prefs[SettingsKeys.dnsPrimaryKey] ?: "1.1.1.1",
                    dnsSecondary = prefs[SettingsKeys.dnsSecondaryKey] ?: "8.8.8.8",
                    themeMode =
                        runCatching {
                            ThemeMode.valueOf(prefs[SettingsKeys.themeModeKey] ?: "SYSTEM")
                        }.getOrDefault(ThemeMode.SYSTEM),
                    languageTag = prefs[SettingsKeys.languageTagKey] ?: "",
                    showNotificationStats = prefs[SettingsKeys.notifStatsKey] ?: true,
                    autoStartOnBoot = prefs[SettingsKeys.autoStartKey] ?: false,
                    blocklistLastUpdatedMs = prefs[SettingsKeys.lastUpdatedMsKey] ?: 0L,
                )
            }

        suspend fun setBlocklistSource(source: BlocklistSource) {
            dataStore.edit {
                it[SettingsKeys.blocklistSourceKey] = source.key
            }
        }

        suspend fun setCustomUrl(url: String) {
            dataStore.edit { it[SettingsKeys.customUrlKey] = url }
        }

        suspend fun setDns(
            primary: String,
            secondary: String,
        ) {
            dataStore.edit {
                it[SettingsKeys.dnsPrimaryKey] = primary
                it[SettingsKeys.dnsSecondaryKey] = secondary
            }
        }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { it[SettingsKeys.themeModeKey] = mode.name }
        }

        suspend fun setLanguageTag(tag: String) {
            dataStore.edit { it[SettingsKeys.languageTagKey] = tag }
        }

        suspend fun setNotificationStats(enabled: Boolean) {
            dataStore.edit { it[SettingsKeys.notifStatsKey] = enabled }
        }

        suspend fun setAutoStart(enabled: Boolean) {
            dataStore.edit { it[SettingsKeys.autoStartKey] = enabled }
        }

        suspend fun setBlocklistLastUpdated(epochMs: Long) {
            dataStore.edit { it[SettingsKeys.lastUpdatedMsKey] = epochMs }
        }
    }
