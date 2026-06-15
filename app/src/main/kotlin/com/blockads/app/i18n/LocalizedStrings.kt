package com.blockads.app.i18n

interface LocalizedStrings {
    // Navigation
    val navHome: String
    val navSettings: String
    val navAbout: String

    // Home screen
    val appName: String
    val vpnActive: String
    val vpnStopped: String
    val vpnConnecting: String
    val vpnError: String
    val blockedCount: String
    val forwardedCount: String
    val sessionUptime: String
    val tapToStart: String
    val tapToStop: String
    val currentBlocklist: String

    // Pause VPN
    val pauseVpn: String
    val resumeVpn: String
    val pauseFor15m: String
    val pauseFor1h: String
    val pauseCustom: String
    val pausedUntil: String

    // Settings
    val settingsTitle: String
    val appearanceSection: String
    val themeLabel: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val languageLabel: String
    val languageSystem: String
    val blocklistSection: String
    val blocklistLabel: String
    val customUrlLabel: String
    val customUrlHint: String
    val upstreamDnsSection: String
    val upstreamDnsLabel: String
    val upstreamDnsHint: String
    val behaviourSection: String
    val autoStartLabel: String
    val autoStartSubtitle: String
    val notificationStatsLabel: String
    val notificationStatsSubtitle: String
    val refreshBlocklistButton: String
    val refreshing: String
    val lastUpdated: String
    val never: String

    // Blocklist source names
    val sourceBundled: String
    val sourceAdGuardDns: String
    val sourceAdGuardHosts: String
    val sourceStevenBlack: String
    val sourceOisd: String
    val sourceHaGeZiPro: String
    val sourceCloudflareFamilies: String
    val sourceQuad9: String
    val sourceCustom: String

    // About
    val aboutTitle: String
    val aboutDeveloper: String
    val aboutDeveloperName: String
    val aboutVersion: String
    val aboutDescription: String
    val aboutOpenSource: String
    val aboutLicenseMit: String
    val aboutLicenseApache: String
    val aboutLicenseGpl: String
    val aboutViewOnGithub: String
    val aboutReportIssue: String

    // Crash
    val crashTitle: String
    val crashSubtitle: String
    val crashDetails: String
    val crashCopy: String
    val crashRestart: String
    val crashCopied: String

    // Errors / status
    val errorVpnPermission: String
    val errorNetworkUnavailable: String
    val errorInvalidUrl: String
    val errorInvalidDns: String
    val errorBlocklistDownload: String
    val errorGeneric: String
    val statusConnected: String
    val statusDisconnected: String
}
