package com.blockads.app.core.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val blocklistSourceKey = stringPreferencesKey("blocklist_source")
    val customUrlKey = stringPreferencesKey("custom_url")
    val dnsPrimaryKey = stringPreferencesKey("dns_primary")
    val dnsSecondaryKey = stringPreferencesKey("dns_secondary")
    val themeModeKey = stringPreferencesKey("theme_mode")
    val languageTagKey = stringPreferencesKey("language_tag")
    val notifStatsKey = booleanPreferencesKey("notif_stats")
    val autoStartKey = booleanPreferencesKey("auto_start")
    val lastUpdatedMsKey = longPreferencesKey("blocklist_last_updated_ms")
}
