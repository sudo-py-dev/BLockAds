package com.blockads.app.core.data.blocklist

sealed class BlocklistSource(val key: String) {
    data object Bundled : BlocklistSource("bundled")

    data object AdGuardDns : BlocklistSource("adguard_dns")

    data object AdGuardHosts : BlocklistSource("adguard_hosts")

    data object StevenBlack : BlocklistSource("steven_black")

    data object Oisd : BlocklistSource("oisd")

    data object HaGeZiPro : BlocklistSource("hagezi_pro")

    data object CloudflareFamilies : BlocklistSource("cloudflare_families")

    data object Quad9 : BlocklistSource("quad9")

    data class Custom(val url: String) : BlocklistSource("custom")

    val upstreamDns: Pair<String, String>?
        get() =
            when (this) {
                is AdGuardDns -> "94.140.14.14" to "94.140.14.15"
                is CloudflareFamilies -> "1.1.1.3" to "1.0.0.3"
                is Quad9 -> "9.9.9.9" to "149.112.112.112"
                else -> null
            }

    val remoteUrl: String?
        get() =
            when (this) {
                is AdGuardHosts -> "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt"
                is StevenBlack -> "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
                is Oisd -> "https://big.oisd.nl/domainswild"
                is HaGeZiPro -> "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt"
                is Custom -> url.takeIf { it.isNotBlank() }
                else -> null
            }

    companion object {
        fun fromKey(
            key: String,
            customUrl: String = "",
        ): BlocklistSource =
            when (key) {
                "bundled" -> Bundled
                "adguard_dns" -> AdGuardDns
                "adguard_hosts" -> AdGuardHosts
                "steven_black" -> StevenBlack
                "oisd" -> Oisd
                "hagezi_pro" -> HaGeZiPro
                "cloudflare_families" -> CloudflareFamilies
                "quad9" -> Quad9
                "custom" -> Custom(customUrl)
                else -> Bundled
            }

        val all: List<BlocklistSource> =
            listOf(
                Bundled, AdGuardDns, AdGuardHosts, StevenBlack,
                Oisd, HaGeZiPro, CloudflareFamilies, Quad9, Custom(""),
            )
    }
}
