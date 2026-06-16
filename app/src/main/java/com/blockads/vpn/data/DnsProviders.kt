package com.blockads.vpn.data

import android.content.Context
import com.blockads.vpn.util.Logger
import org.json.JSONArray

data class DnsProvider(
    val id: String,
    val name: String,
    val dohUrl: String,
    val dotIp: String,
    val privacyUrl: String
)

object DnsProviders {
    private val _providers = mutableListOf<DnsProvider>()
    val providers: List<DnsProvider> get() = _providers

    fun init(context: Context) {
        if (_providers.isNotEmpty()) return
        try {
            val inputStream = context.assets.open("providers.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                _providers.add(
                    DnsProvider(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        dohUrl = obj.getString("dohUrl"),
                        dotIp = obj.getString("dotIp"),
                        privacyUrl = obj.getString("privacyUrl")
                    )
                )
            }
        } catch (e: Exception) {
            Logger.e("DnsProviders", "Failed to load providers.json", e)
            // Fallback provider in case JSON fails
            _providers.add(
                DnsProvider(
                    id = "adguard_recommended",
                    name = "AdGuard Recommended",
                    dohUrl = "https://dns.adguard-dns.com/dns-query",
                    dotIp = "94.140.14.14",
                    privacyUrl = "https://adguard-dns.io/en/privacy.html"
                )
            )
        }
    }

    fun getProviderById(id: String): DnsProvider? {
        return providers.find { it.id == id }
    }
}
