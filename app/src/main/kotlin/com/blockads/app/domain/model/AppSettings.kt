package com.blockads.app.domain.model

import com.blockads.app.core.data.blocklist.BlocklistSource

data class AppSettings(
    val blocklistSource: BlocklistSource = BlocklistSource.Bundled,
    val customBlocklistUrl: String = "",
    val dnsPrimary: String = "1.1.1.1",
    val dnsSecondary: String = "8.8.8.8",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageTag: String = "",
    val showNotificationStats: Boolean = true,
    val autoStartOnBoot: Boolean = false,
    val blocklistLastUpdatedMs: Long = 0L,
) {
    companion object {
        fun default(): AppSettings = AppSettings()
    }
}
