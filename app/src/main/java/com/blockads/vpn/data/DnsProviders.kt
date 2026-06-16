package com.blockads.vpn.data

object DnsProviders {
    val providers =
        mapOf(
            "AdGuard DNS 1" to "94.140.14.14",
            "AdGuard DNS 2" to "94.140.15.15",
            "AdGuard Family" to "94.140.14.15",
            "Control D (Ad Block)" to "76.76.2.2",
            "Mullvad (Ad Block)" to "194.242.2.3",
            "Mullvad (Ad & Tracker Block)" to "194.242.2.4",
            "Alternate DNS" to "76.76.19.19",
            "Alternate DNS 2" to "76.76.20.20",
        )

    fun getIpByName(name: String): String {
        return providers[name] ?: "94.140.14.14"
    }

    fun getNameByIp(ip: String): String? {
        return providers.entries.find { it.value == ip }?.key
    }
}
