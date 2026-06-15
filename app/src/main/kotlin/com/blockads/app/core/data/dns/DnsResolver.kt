package com.blockads.app.core.data.dns

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DnsResolver"
private val DNS_MESSAGE_TYPE = "application/dns-message".toMediaType()

@Singleton
class DnsResolver @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun forward(
        queryPacket: ByteArray,
        primaryDns: String,
        secondaryDns: String,
    ): ByteArray? = withContext(Dispatchers.IO) {
        // Try DoH first for security and privacy
        resolveViaDoh(queryPacket, primaryDns)
            ?: resolveViaDoh(queryPacket, secondaryDns)
    }

    private fun resolveViaDoh(
        queryPacket: ByteArray,
        dnsServer: String,
    ): ByteArray? {
        // Simple heuristic: if it looks like an IP, it's probably not a DoH endpoint
        // Production ad blockers should use pre-configured DoH URLs (Cloudflare, Google, etc.)
        val dohUrl = when (dnsServer) {
            "1.1.1.1", "1.0.0.1" -> "https://cloudflare-dns.com/dns-query"
            "8.8.8.8", "8.8.4.4" -> "https://dns.google/dns-query"
            else -> if (dnsServer.startsWith("https://")) dnsServer else null
        } ?: return null

        return runCatching {
            val request = Request.Builder()
                .url(dohUrl)
                .header("Accept", "application/dns-message")
                .post(queryPacket.toRequestBody(DNS_MESSAGE_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "DoH request failed for $dnsServer: ${response.code}")
                    return@runCatching null
                }
                response.body?.bytes()
            }
        }.onFailure { e ->
            Log.w(TAG, "DoH resolution error for $dnsServer", e)
        }.getOrNull()
    }
}

