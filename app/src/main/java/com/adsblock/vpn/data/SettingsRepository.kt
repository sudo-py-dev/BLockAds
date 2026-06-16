package com.adsblock.vpn.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val DNS_PROVIDER_ID_KEY = stringPreferencesKey("dns_provider_id")
        val DNS_PROTOCOL_KEY = stringPreferencesKey("dns_protocol") // "doh" or "dot"
        val THEME_KEY = stringPreferencesKey("theme")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val AUTO_CONNECT_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("auto_connect_on_boot")

        const val DEFAULT_DNS_ID = "adguard_recommended"
        const val DEFAULT_PROTOCOL = "doh"
    }

    val dnsProviderId: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[DNS_PROVIDER_ID_KEY] ?: DEFAULT_DNS_ID
        }

    val dnsProtocol: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[DNS_PROTOCOL_KEY] ?: DEFAULT_PROTOCOL
        }

    val theme: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    val language: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: "system"
        }

    val autoConnectOnBoot: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[AUTO_CONNECT_KEY] ?: false
        }

    suspend fun setDnsProviderId(providerId: String) {
        context.dataStore.edit { preferences ->
            preferences[DNS_PROVIDER_ID_KEY] = providerId
        }
    }

    suspend fun setDnsProtocol(protocol: String) {
        context.dataStore.edit { preferences ->
            preferences[DNS_PROTOCOL_KEY] = protocol
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun setAutoConnectOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CONNECT_KEY] = enabled
        }
    }
}
