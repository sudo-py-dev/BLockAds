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
class DnsResolver
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
    ) {
        suspend fun forward(
            queryPacket: ByteArray,
            primaryDns: String,
            secondaryDns: String,
        ): ByteArray? =
            withContext(Dispatchers.IO) {
                // Try DoH first for security and privacy
                resolveViaDoh(queryPacket, primaryDns)
                    ?: resolveViaDoh(queryPacket, secondaryDns)
                    ?: resolveViaUdp(queryPacket, primaryDns)
                    ?: resolveViaUdp(queryPacket, secondaryDns)
            }

        private fun resolveViaUdp(
            queryPacket: ByteArray,
            dnsServer: String,
        ): ByteArray? {
            return runCatching {
                java.net.DatagramSocket().use { socket ->
                    socket.soTimeout = 5000
                    val address = java.net.InetAddress.getByName(dnsServer)
                    val packet = java.net.DatagramPacket(queryPacket, queryPacket.size, address, 53)
                    socket.send(packet)

                    val buffer = ByteArray(4096)
                    val response = java.net.DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    buffer.copyOfRange(0, response.length)
                }
            }.onFailure { e ->
                Log.w(TAG, "UDP resolution error for $dnsServer", e)
            }.getOrNull()
        }

        private fun resolveViaDoh(
            queryPacket: ByteArray,
            dnsServer: String,
        ): ByteArray? {
            val dohUrl =
                when (dnsServer) {
                    "1.1.1.1", "1.0.0.1" -> "https://cloudflare-dns.com/dns-query"
                    "1.1.1.2", "1.0.0.2" -> "https://security.cloudflare-dns.com/dns-query"
                    "1.1.1.3", "1.0.0.3" -> "https://family.cloudflare-dns.com/dns-query"
                    "8.8.8.8", "8.8.4.4" -> "https://dns.google/dns-query"
                    "9.9.9.9", "149.112.112.112" -> "https://dns.quad9.net/dns-query"
                    "94.140.14.14", "94.140.14.15" -> "https://dns.adguard-dns.com/dns-query"
                    "94.140.14.140", "94.140.14.141" -> "https://dns-family.adguard-dns.com/dns-query"
                    else -> if (dnsServer.startsWith("https://")) dnsServer else null
                } ?: return null

            return runCatching {
                val request =
                    Request.Builder()
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
