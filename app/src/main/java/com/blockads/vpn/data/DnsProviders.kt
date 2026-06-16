package com.blockads.vpn.data

import com.blockads.vpn.R

data class DnsProvider(
    val nameResId: Int,
    val ip: String
)

object DnsProviders {
    val providers = listOf(
        DnsProvider(R.string.dns_adguard_recommended, "94.140.14.14"),
        DnsProvider(R.string.dns_adguard_2, "94.140.15.15"),
        DnsProvider(R.string.dns_adguard_family, "94.140.14.15"),
        DnsProvider(R.string.dns_adguard_family_2, "94.140.15.16"),
        DnsProvider(R.string.dns_controld_adblock, "76.76.2.2"),
        DnsProvider(R.string.dns_mullvad_adblock, "194.242.2.3"),
        DnsProvider(R.string.dns_mullvad_tracker, "194.242.2.4"),
        DnsProvider(R.string.dns_cloudflare_security, "1.1.1.2"),
        DnsProvider(R.string.dns_cloudflare_family, "1.1.1.3"),
        DnsProvider(R.string.dns_cleanbrowsing_family, "185.228.168.168"),
        DnsProvider(R.string.dns_cleanbrowsing_adult, "185.228.168.10"),
        DnsProvider(R.string.dns_quad9_malware, "9.9.9.9"),
        DnsProvider(R.string.dns_alternate_dns, "76.76.19.19"),
        DnsProvider(R.string.dns_alternate_dns_2, "76.76.20.20")
    )

    fun getIpByNameResId(nameResId: Int): String {
        return providers.find { it.nameResId == nameResId }?.ip ?: "94.140.14.14"
    }

    fun getNameResByIp(ip: String): Int? {
        return providers.find { it.ip == ip }?.nameResId
    }
}
