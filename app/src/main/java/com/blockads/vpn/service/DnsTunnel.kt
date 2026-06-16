package com.blockads.vpn.service

import android.net.VpnService
import com.blockads.vpn.data.DnsLogManager
import com.blockads.vpn.data.DnsStatsManager
import com.blockads.vpn.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xbill.DNS.ARecord
import org.xbill.DNS.Message
import org.xbill.DNS.Rcode
import org.xbill.DNS.Section
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class DnsTunnel(
    private val vpnService: VpnService,
    private val tunFd: java.io.FileDescriptor,
    private val upstreamDnsIp: String,
    private val scope: CoroutineScope,
    private val onFallbackStateChanged: ((Boolean) -> Unit)? = null,
) {
    private var isRunning = false

    @Volatile
    var isPaused = false
    private val upstreamAddress = InetAddress.getByName(upstreamDnsIp)

    @Volatile
    var isFallbackActive = false
    private var consecutiveTimeouts = 0
    private val timeoutThreshold = 5
    private val fallbackAddress = InetAddress.getByName("94.140.14.14") // AdGuard DNS

    // Track pending DNS requests: Transaction ID -> ClientInfo
    private val requestMap = java.util.concurrent.ConcurrentHashMap<Short, ClientInfo>()

    data class ClientInfo(val sourceIp: Int, val sourcePort: Short, val destIp: Int, val destPort: Short, val domain: String, val timestamp: Long)

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch(Dispatchers.IO) {
            val tunInput = FileInputStream(tunFd)
            val tunOutput = FileOutputStream(tunFd)
            val buffer = ByteArray(32767)

            val udpSocket = DatagramSocket()
            vpnService.protect(udpSocket)

            // Receiver for upstream DNS responses
            launch(Dispatchers.IO) {
                val recvBuffer = ByteArray(4096)
                val udpPacket = DatagramPacket(recvBuffer, recvBuffer.size)
                while (isRunning) {
                    try {
                        udpSocket.receive(udpPacket)
                        
                        // Security check: Verify response comes from the expected upstream DNS server
                        val expectedAddress = if (isFallbackActive) fallbackAddress else upstreamAddress
                        if (udpPacket.address != expectedAddress || udpPacket.port != 53) {
                            continue 
                        }

                        val responseLen = udpPacket.length
                        if (responseLen >= 2) {
                            val txId = ByteBuffer.wrap(recvBuffer, 0, 2).getShort()
                            val clientInfo = requestMap.remove(txId)
                            if (clientInfo != null) {
                                consecutiveTimeouts = 0 // Reset timeouts on successful response
                                val payload = recvBuffer.copyOfRange(0, responseLen)

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
                                tunOutput.write(responsePacket)
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning) Logger.e("DnsTunnel", "Error receiving from upstream", e)
                    }
                }
            }

            // Timeout tracker
            launch(Dispatchers.Default) {
                while (isRunning) {
                    val now = System.currentTimeMillis()
                    var timeoutsInThisTick = 0
                    val iter = requestMap.iterator()
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        if (now - entry.value.timestamp > 3000) { // 3 seconds timeout
                            iter.remove()
                            timeoutsInThisTick++
                        }
                    }
                    if (timeoutsInThisTick > 0 && !isFallbackActive) {
                        consecutiveTimeouts += timeoutsInThisTick
                        if (consecutiveTimeouts >= timeoutThreshold) {
                            isFallbackActive = true
                            onFallbackStateChanged?.invoke(true)
                        }
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            // Recovery polling
            launch(Dispatchers.IO) {
                while (isRunning) {
                    if (isFallbackActive) {
                        try {
                            val testSocket = DatagramSocket()
                            vpnService.protect(testSocket)
                            testSocket.soTimeout = 2000
                            
                            val record = org.xbill.DNS.Record.newRecord(org.xbill.DNS.Name.fromString("google.com."), org.xbill.DNS.Type.A, org.xbill.DNS.DClass.IN)
                            val query = org.xbill.DNS.Message.newQuery(record)
                            val queryBytes = query.toWire()
                            
                            val packet = DatagramPacket(queryBytes, queryBytes.size, upstreamAddress, 53)
                            testSocket.send(packet)
                            
                            val responseBuf = ByteArray(1024)
                            val respPacket = DatagramPacket(responseBuf, responseBuf.size)
                            testSocket.receive(respPacket)
                            
                            isFallbackActive = false
                            consecutiveTimeouts = 0
                            onFallbackStateChanged?.invoke(false)
                            
                            testSocket.close()
                        } catch (e: Exception) {
                            // Still down
                        }
                    }
                    kotlinx.coroutines.delay(if (isFallbackActive) 10000 else 1000)
                }
            }

            while (isRunning) {
                try {
                    val length = tunInput.read(buffer)
                    if (length > 0) {
                        handlePacket(buffer, length, udpSocket)
                    }
                } catch (e: Exception) {
                    if (isRunning) Logger.e("DnsTunnel", "Error reading TUN", e)
                }
            }
            udpSocket.close()
        }
    }

    fun stop() {
        isRunning = false
    }

    private suspend fun handlePacket(
        packet: ByteArray,
        length: Int,
        udpSocket: DatagramSocket,
    ) {
        if (length < 28) return // IP header + UDP header min size
        val buffer = ByteBuffer.wrap(packet, 0, length)

        val versionAndIhl = buffer.get(0).toInt()
        val version = versionAndIhl shr 4
        if (version != 4) return // Only support IPv4
        val ihl = versionAndIhl and 0x0F
        val ipHeaderLen = ihl * 4
        if (ihl < 5 || ipHeaderLen + 8 > length) return

        val protocol = buffer.get(9).toInt()
        if (protocol != 17) return // Only support UDP

        val sourceIp = buffer.getInt(12)
        val destIp = buffer.getInt(16)

        val sourcePort = buffer.getShort(ipHeaderLen)
        val destPort = buffer.getShort(ipHeaderLen + 2)

        if (destPort.toInt() != 53) return // Only support DNS queries

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
            requestMap[txId] = ClientInfo(sourceIp, sourcePort, destIp, destPort, domain, System.currentTimeMillis())

            DnsStatsManager.incrementTotal()

            val activeUpstream = if (isFallbackActive) fallbackAddress else upstreamAddress
            val outAddress = if (isPaused) InetAddress.getByName("1.1.1.1") else activeUpstream
            val outPacket = DatagramPacket(payload, payloadLen, outAddress, 53)
            withContext(Dispatchers.IO) {
                udpSocket.send(outPacket)
            }
        }
    }
}
