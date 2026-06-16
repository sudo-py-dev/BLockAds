package com.adsblock.vpn.service

import android.net.VpnService
import com.adsblock.vpn.data.DnsLogManager
import com.adsblock.vpn.data.DnsStatsManager
import com.adsblock.vpn.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xbill.DNS.ARecord
import org.xbill.DNS.Message
import org.xbill.DNS.Rcode
import org.xbill.DNS.Section
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class DnsTunnel(
    private val vpnService: VpnService,
    private val tunFd: java.io.FileDescriptor,
    private val upstreamEndpoint: String,
    private val protocol: String,
    private val scope: CoroutineScope,
    private val onFallbackStateChanged: ((Boolean) -> Unit)? = null,
) {
    private var isRunning = false

    @Volatile
    var isPaused = false

    @Volatile
    var isFallbackActive = false
    private var lastSuccessTime = System.currentTimeMillis()
    private var fallbackStartTime = 0L

    private val fallbackDohUrl = "https://dns.adguard-dns.com/dns-query"
    private val fallbackDotIp = "94.140.14.14"

    private val pausedDohUrl = "https://cloudflare-dns.com/dns-query"
    private val pausedDotIp = "1.1.1.1"

    private object BootstrapDns : okhttp3.Dns {
        private val cache = mutableMapOf<String, List<java.net.InetAddress>>()
        override fun lookup(hostname: String): List<java.net.InetAddress> {
            cache[hostname]?.let { return it }
            try {
                val lookup = org.xbill.DNS.Lookup(hostname, org.xbill.DNS.Type.A)
                val resolver = org.xbill.DNS.SimpleResolver("8.8.8.8")
                resolver.timeout = java.time.Duration.ofSeconds(3)
                lookup.setResolver(resolver)
                val records = lookup.run()
                if (lookup.result == org.xbill.DNS.Lookup.SUCCESSFUL && records != null) {
                    val addresses = records.filterIsInstance<ARecord>().map { it.address }
                    if (addresses.isNotEmpty()) {
                        cache[hostname] = addresses
                        return addresses
                    }
                }
            } catch (e: Exception) {}
            return okhttp3.Dns.SYSTEM.lookup(hostname)
        }
    }

    private fun resolveHost(host: String): String {
        if (host.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) return host
        return try {
            BootstrapDns.lookup(host).firstOrNull()?.hostAddress ?: host
        } catch (e: Exception) {
            host
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dns(BootstrapDns)
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    // Persistent connection for DoT
    private var dotSocket: SSLSocket? = null
    private val dotMutex = Mutex()

    data class ClientInfo(
        val sourceIp: Int,
        val sourcePort: Short,
        val destIp: Int,
        val destPort: Short,
        val domain: String,
        val timestamp: Long,
        val txId: Short
    )

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch(Dispatchers.IO) {
            val tunInput = FileInputStream(tunFd)
            val tunOutput = FileOutputStream(tunFd)
            val buffer = ByteArray(32767)

            // Health Monitor
            launch(Dispatchers.Default) {
                while (isRunning) {
                    val now = System.currentTimeMillis()
                    if (!isFallbackActive && !isPaused) {
                        if (now - lastSuccessTime > 4000) {
                            val isAlive = checkUpstreamHealth(upstreamEndpoint, protocol)
                            if (!isAlive) {
                                isFallbackActive = true
                                fallbackStartTime = System.currentTimeMillis()
                                onFallbackStateChanged?.invoke(true)
                            } else {
                                lastSuccessTime = System.currentTimeMillis()
                            }
                        }
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            // Recovery polling
            launch(Dispatchers.IO) {
                while (isRunning) {
                    if (isFallbackActive && !isPaused) {
                        val now = System.currentTimeMillis()
                        if (now - fallbackStartTime > 30_000) {
                            val isAlive = checkUpstreamHealth(upstreamEndpoint, protocol)
                            if (isAlive) {
                                isFallbackActive = false
                                lastSuccessTime = System.currentTimeMillis()
                                onFallbackStateChanged?.invoke(false)
                            }
                        }
                    }
                    kotlinx.coroutines.delay(if (isFallbackActive) 10000 else 1000)
                }
            }

            while (isRunning) {
                try {
                    val length = tunInput.read(buffer)
                    if (length > 0) {
                        val packetCopy = buffer.copyOfRange(0, length)
                        handlePacket(packetCopy, length, tunOutput)
                    }
                } catch (e: Exception) {
                    if (isRunning) Logger.e("DnsTunnel", "Error reading TUN", e)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            dotSocket?.close()
        } catch (e: Exception) {}
    }

    private fun handlePacket(
        packet: ByteArray,
        length: Int,
        tunOutput: FileOutputStream,
    ) {
        if (length < 28) return
        val buffer = ByteBuffer.wrap(packet, 0, length)

        val versionAndIhl = buffer.get(0).toInt()
        val version = versionAndIhl shr 4
        if (version != 4) return
        val ihl = versionAndIhl and 0x0F
        val ipHeaderLen = ihl * 4
        if (ihl < 5 || ipHeaderLen + 8 > length) return

        val protocolByte = buffer.get(9).toInt()
        if (protocolByte != 17) return

        val sourceIp = buffer.getInt(12)
        val destIp = buffer.getInt(16)

        val sourcePort = buffer.getShort(ipHeaderLen)
        val destPort = buffer.getShort(ipHeaderLen + 2)

        if (destPort.toInt() != 53) return

        val udpLength = buffer.getShort(ipHeaderLen + 4).toInt() and 0xFFFF
        val payloadLen = udpLength - 8
        if (payloadLen <= 0 || ipHeaderLen + 8 + payloadLen > length) return

        val payload = ByteArray(payloadLen)
        buffer.position(ipHeaderLen + 8)
        buffer.get(payload)

        if (payloadLen >= 2) {
            val domain = try {
                val message = Message(payload)
                message.question?.name?.toString(true) ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }

            val txId = ByteBuffer.wrap(payload, 0, 2).getShort()
            val clientInfo = ClientInfo(sourceIp, sourcePort, destIp, destPort, domain, System.currentTimeMillis(), txId)

            DnsStatsManager.incrementTotal()

            if (protocol == "dot") {
                val activeIp = if (isPaused) pausedDotIp else if (isFallbackActive) fallbackDotIp else upstreamEndpoint
                sendDoTRequest(activeIp, payload, clientInfo, tunOutput)
            } else {
                val activeUrl = if (isPaused) pausedDohUrl else if (isFallbackActive) fallbackDohUrl else upstreamEndpoint
                sendDoHRequest(activeUrl, payload, clientInfo, tunOutput)
            }
        }
    }

    private fun sendDoHRequest(url: String, payload: ByteArray, clientInfo: ClientInfo, tunOutput: FileOutputStream) {
        val mediaType = "application/dns-message".toMediaType()
        val requestBody = payload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Accept", "application/dns-message")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (isRunning) Logger.w("DnsTunnel", "DoH request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) return
                    val responseBytes = it.body?.bytes()
                    if (responseBytes != null && responseBytes.size >= 2) {
                        processDnsResponse(responseBytes, clientInfo, tunOutput)
                    }
                }
            }
        })
    }

    private fun sendDoTRequest(ip: String, payload: ByteArray, clientInfo: ClientInfo, tunOutput: FileOutputStream) {
        scope.launch(Dispatchers.IO) {
            try {
                dotMutex.withLock {
                    if (dotSocket == null || dotSocket?.isClosed == true) {
                        val resolvedIp = resolveHost(ip)
                        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                        val underlyingSocket = java.net.Socket()
                        underlyingSocket.connect(java.net.InetSocketAddress(resolvedIp, 853), 5000)
                        val socket = factory.createSocket(underlyingSocket, ip, 853, true) as SSLSocket
                        socket.soTimeout = 5000
                        socket.startHandshake()
                        dotSocket = socket
                    }

                    val socket = dotSocket!!
                    val outStream = socket.outputStream
                    val inStream = socket.inputStream

                    val lengthBytes = ByteBuffer.allocate(2).putShort(payload.size.toShort()).array()
                    outStream.write(lengthBytes)
                    outStream.write(payload)
                    outStream.flush()

                    val respLenBytes = ByteArray(2)
                    var bytesRead = inStream.read(respLenBytes)
                    if (bytesRead == 2) {
                        val respLen = ByteBuffer.wrap(respLenBytes).short.toInt() and 0xFFFF
                        val respPayload = ByteArray(respLen)
                        var totalRead = 0
                        while (totalRead < respLen) {
                            val read = inStream.read(respPayload, totalRead, respLen - totalRead)
                            if (read == -1) break
                            totalRead += read
                        }
                        if (totalRead == respLen) {
                            processDnsResponse(respPayload, clientInfo, tunOutput)
                        }
                    } else {
                        dotSocket?.close()
                        dotSocket = null
                    }
                }
            } catch (e: Exception) {
                dotSocket?.close()
                dotSocket = null
                if (isRunning) Logger.w("DnsTunnel", "DoT request failed: ${e.message}")
            }
        }
    }

    private suspend fun checkUpstreamHealth(endpoint: String, protocol: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = org.xbill.DNS.Record.newRecord(org.xbill.DNS.Name.fromString("google.com."), org.xbill.DNS.Type.A, org.xbill.DNS.DClass.IN)
            val query = org.xbill.DNS.Message.newQuery(record)
            val payload = query.toWire()

            if (protocol == "doh") {
                val mediaType = "application/dns-message".toMediaType()
                val requestBody = payload.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .header("Accept", "application/dns-message")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                response.isSuccessful
            } else {
                dotMutex.withLock {
                    if (dotSocket == null || dotSocket?.isClosed == true) {
                        val resolvedIp = resolveHost(endpoint)
                        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                        val underlyingSocket = java.net.Socket()
                        underlyingSocket.connect(java.net.InetSocketAddress(resolvedIp, 853), 5000)
                        val socket = factory.createSocket(underlyingSocket, endpoint, 853, true) as SSLSocket
                        socket.soTimeout = 5000
                        socket.startHandshake()
                        dotSocket = socket
                    }
                    val socket = dotSocket!!
                    val outStream = socket.outputStream
                    val lengthBytes = ByteBuffer.allocate(2).putShort(payload.size.toShort()).array()
                    outStream.write(lengthBytes)
                    outStream.write(payload)
                    outStream.flush()

                    val inStream = socket.inputStream
                    val respLenBytes = ByteArray(2)
                    val bytesRead = inStream.read(respLenBytes)
                    bytesRead == 2
                }
            }
        } catch (e: Exception) {
            dotSocket?.close()
            dotSocket = null
            false
        }
    }

    private fun processDnsResponse(payload: ByteArray, clientInfo: ClientInfo, tunOutput: FileOutputStream) {
        lastSuccessTime = System.currentTimeMillis()

        if (payload.size >= 2) {
            val buf = ByteBuffer.wrap(payload)
            buf.putShort(0, clientInfo.txId)
        }

        try {
            val message = Message(payload)
            val rcode = message.header.rcode
            if (rcode == Rcode.NXDOMAIN) {
                DnsStatsManager.incrementBlocked()
                DnsLogManager.addLog(clientInfo.domain, true)
            } else {
                var isBlocked = false
                if (!isPaused) {
                    val answers = message.getSection(Section.ANSWER)
                    for (record in answers) {
                        if (record is ARecord && record.address.hostAddress == "0.0.0.0") {
                            isBlocked = true
                            break
                        }
                    }
                    if (isBlocked) DnsStatsManager.incrementBlocked()
                }
                DnsLogManager.addLog(clientInfo.domain, isBlocked)
            }
        } catch (e: Exception) {
            if (isRunning) Logger.w("DnsTunnel", "Failed to parse upstream response", e)
        }

        val responsePacket =
            IpUtil.buildUdpIpPacket(
                sourceIp = clientInfo.destIp,
                destIp = clientInfo.sourceIp,
                sourcePort = clientInfo.destPort,
                destPort = clientInfo.sourcePort,
                payload = payload,
            )
        try {
            tunOutput.write(responsePacket)
        } catch (e: Exception) {
            if (isRunning) Logger.e("DnsTunnel", "Error writing to TUN", e)
        }
    }
}
