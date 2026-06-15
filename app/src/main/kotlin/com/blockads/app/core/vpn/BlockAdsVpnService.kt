package com.blockads.app.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.blockads.app.MainActivity
import com.blockads.app.core.data.blocklist.BlocklistRepository
import com.blockads.app.core.data.blocklist.BlocklistSource
import com.blockads.app.core.data.dns.DnsLogRepository
import com.blockads.app.core.data.dns.DnsPacketParser
import com.blockads.app.core.data.dns.DnsResolver
import com.blockads.app.core.data.dns.DnsResponseBuilder
import com.blockads.app.core.data.dns.ParseResult
import com.blockads.app.core.data.settings.SettingsRepository
import com.blockads.app.domain.model.AppSettings
import com.blockads.app.domain.model.DnsStats
import com.blockads.app.domain.model.VpnState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "BlockAdsVpnService"
private const val NOTIF_ID = 1001
private const val CHANNEL_ID = "blockads_vpn"
private const val VPN_ADDRESS = "10.33.33.1"
private const val VPN_DNS = "10.33.33.1"
private const val MTU = 1500

@AndroidEntryPoint
class BlockAdsVpnService : VpnService() {
    @Inject lateinit var blocklistRepository: BlocklistRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var dnsResolver: DnsResolver

    @Inject lateinit var dnsLogRepository: DnsLogRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunFd: ParcelFileDescriptor? = null
    private var statsJob: Job? = null

    @Volatile private var currentSettings: AppSettings = AppSettings.default()

    companion object {
        const val ACTION_START = "com.blockads.app.START_VPN"
        const val ACTION_STOP = "com.blockads.app.STOP_VPN"
        const val ACTION_PAUSE = "com.blockads.app.PAUSE_VPN"
        const val ACTION_RESUME = "com.blockads.app.RESUME_VPN"
        const val EXTRA_PAUSE_DURATION_MS = "pause_duration_ms"

        private val _vpnState = MutableStateFlow(VpnState.STOPPED)
        val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

        private val _stats = MutableStateFlow(DnsStats())
        val stats: StateFlow<DnsStats> = _stats.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private var pauseUntilMs: Long = 0L
    private var pauseJob: Job? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            ACTION_PAUSE -> {
                val duration = intent.getLongExtra(EXTRA_PAUSE_DURATION_MS, 0L)
                if (duration > 0) pauseVpn(duration)
            }
            ACTION_RESUME -> resumeVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun pauseVpn(durationMs: Long) {
        if (_vpnState.value != VpnState.ACTIVE && _vpnState.value != VpnState.PAUSED) return
        pauseUntilMs = System.currentTimeMillis() + durationMs
        _vpnState.value = VpnState.PAUSED

        val notifManager = getSystemService(NotificationManager::class.java)
        notifManager.notify(NOTIF_ID, buildNotification(_stats.value.blockedCount))

        pauseJob?.cancel()
        pauseJob =
            scope.launch {
                delay(durationMs)
                resumeVpn()
            }
    }

    private fun resumeVpn() {
        if (_vpnState.value != VpnState.PAUSED) return
        pauseUntilMs = 0L
        pauseJob?.cancel()
        pauseJob = null
        _vpnState.value = VpnState.ACTIVE

        val notifManager = getSystemService(NotificationManager::class.java)
        notifManager.notify(NOTIF_ID, buildNotification(_stats.value.blockedCount))
    }

    private fun startVpn() {
        if (_vpnState.value == VpnState.ACTIVE || _vpnState.value == VpnState.PAUSED) return
        _vpnState.value = VpnState.CONNECTING
        _stats.value = DnsStats(sessionStartMs = System.currentTimeMillis())

        startForeground(NOTIF_ID, buildNotification(0L))

        scope.launch {
            settingsRepository.appSettings.collect { settings ->
                currentSettings = settings
            }
        }

        scope.launch {
            val settings = settingsRepository.appSettings.first()
            val loadResult = blocklistRepository.refresh(settings.blocklistSource)
            if (loadResult.isFailure) {
                blocklistRepository.ensureLoaded(BlocklistSource.Bundled)
            }

            runCatching { establishTun(settings) }
                .onSuccess { fd ->
                    tunFd = fd
                    _vpnState.value = VpnState.ACTIVE
                    startStatsNotificationUpdater(settings)
                    runPacketLoop(fd)
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to establish VPN tunnel", e)
                    _vpnState.value = VpnState.ERROR
                    stopSelf()
                }
        }
    }

    private fun establishTun(settings: AppSettings): ParcelFileDescriptor {
        val builder =
            Builder()
                .addAddress(VPN_ADDRESS, 32)
                .addAddress("fd00:33:33::1", 128)
                .addDnsServer(VPN_DNS)
                .addDnsServer("fd00:33:33::1")
                .addRoute(VPN_DNS, 32)
                .addRoute("fd00:33:33::1", 128)
                .setMtu(MTU)
                .setSession("BlockAds")

        builder.addDisallowedApplication(packageName)
        settings.bypassedApps.forEach { pkg ->
            try {
                builder.addDisallowedApplication(pkg)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to bypass app: $pkg", e)
            }
        }

        return builder.establish() ?: error("VpnService.Builder.establish() returned null")
    }

    private suspend fun runPacketLoop(fd: ParcelFileDescriptor) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(MTU)

        while (scope.isActive && (_vpnState.value == VpnState.ACTIVE || _vpnState.value == VpnState.PAUSED)) {
            try {
                val len = input.read(buffer)
                if (len <= 0) continue

                val version = (buffer[0].toInt() and 0xF0) shr 4
                val isIpv4 = version == 4
                val isIpv6 = version == 6

                if (!isIpv4 && !isIpv6) continue

                var protocol: Int
                var ihl: Int
                if (isIpv4) {
                    protocol = buffer[9].toInt() and 0xFF
                    ihl = (buffer[0].toInt() and 0x0F) * 4
                } else {
                    // IPv6 handling with potential extension headers
                    var nextHeader = buffer[6].toInt() and 0xFF
                    var offset = 40

                    while (offset < len) {
                        if (nextHeader == 17) break // UDP found

                        // Handle common extension headers: Hop-by-Hop (0), Routing (43),
                        // Fragment (44), Destination Options (60)
                        if (nextHeader == 0 || nextHeader == 43 || nextHeader == 60) {
                            if (offset + 8 > len) break
                            val extLen = (buffer[offset + 1].toInt() and 0xFF + 1) * 8
                            nextHeader = buffer[offset].toInt() and 0xFF
                            offset += extLen
                        } else if (nextHeader == 44) { // Fragment
                            if (offset + 8 > len) break
                            nextHeader = buffer[offset].toInt() and 0xFF
                            offset += 8
                        } else {
                            break // Unknown or non-skippable header
                        }
                    }
                    protocol = nextHeader
                    ihl = offset
                }

                if (protocol != 17) continue // UDP only

                if (len < ihl + 8) continue

                val destPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
                if (destPort != 53) continue

                val udpPayloadLen = ((buffer[ihl + 4].toInt() and 0xFF) shl 8) or (buffer[ihl + 5].toInt() and 0xFF) - 8
                if (udpPayloadLen <= 0 || ihl + 8 + udpPayloadLen > len) continue

                val dnsQuery = buffer.copyOfRange(ihl + 8, ihl + 8 + udpPayloadLen)

                when (val result = DnsPacketParser.parseQuery(dnsQuery)) {
                    is ParseResult.Success -> {
                        val isPaused = _vpnState.value == VpnState.PAUSED && System.currentTimeMillis() < pauseUntilMs
                        val domain = result.domain

                        val isWhitelisted = currentSettings.whitelistDomains.contains(domain)
                        val isBlacklisted = currentSettings.blacklistDomains.contains(domain)

                        val shouldBlock = !isPaused && !isWhitelisted && (isBlacklisted || blocklistRepository.isDomainBlocked(domain))

                        val dnsResponse =
                            if (shouldBlock) {
                                _stats.update { it.copy(blockedCount = it.blockedCount + 1) }
                                dnsLogRepository.logQuery(domain, true)
                                DnsResponseBuilder.nxdomain(result.txId, dnsQuery)
                            } else {
                                _stats.update { it.copy(forwardedCount = it.forwardedCount + 1) }
                                dnsLogRepository.logQuery(domain, false)

                                val primary = currentSettings.blocklistSource.upstreamDns?.first ?: currentSettings.dnsPrimary
                                val secondary = currentSettings.blocklistSource.upstreamDns?.second ?: currentSettings.dnsSecondary
                                dnsResolver.forward(dnsQuery, primary, secondary)
                            }

                        if (dnsResponse != null) {
                            val ipResponse = wrapInIpUdp(buffer, len, ihl, dnsResponse, isIpv4)
                            if (ipResponse != null) {
                                output.write(ipResponse)
                            }
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                if (!scope.isActive) break
                Log.e(TAG, "Error in packet loop", e)
                delay(100)
            }
        }
    }

    private fun startStatsNotificationUpdater(settings: AppSettings) {
        statsJob =
            scope.launch {
                while (isActive) {
                    delay(5_000)
                    if (settings.showNotificationStats) {
                        val notifManager = getSystemService(NotificationManager::class.java)
                        notifManager.notify(NOTIF_ID, buildNotification(_stats.value.blockedCount))
                    }
                }
            }
    }

    private fun stopVpn() {
        _vpnState.value = VpnState.STOPPED
        pauseJob?.cancel()
        pauseJob = null
        pauseUntilMs = 0L
        statsJob?.cancel()
        runCatching { tunFd?.close() }
        tunFd = null
        _stats.value = DnsStats()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
    }

    private fun buildNotification(blockedCount: Long): Notification {
        val openIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(openIntent)
                .setOngoing(true)

        when (_vpnState.value) {
            VpnState.PAUSED -> {
                builder.setContentTitle("BlockAds Paused")
                builder.setContentText("Ad blocking is temporarily disabled")

                val resumeIntent =
                    PendingIntent.getService(
                        this,
                        2,
                        Intent(this, BlockAdsVpnService::class.java).setAction(ACTION_RESUME),
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                builder.addAction(0, "Resume", resumeIntent)
            }
            else -> {
                builder.setContentTitle("BlockAds")
                builder.setContentText("Blocking ads · $blockedCount blocked")

                val pauseIntent =
                    PendingIntent.getService(
                        this,
                        1,
                        Intent(this, BlockAdsVpnService::class.java).apply {
                            action = ACTION_PAUSE
                            putExtra(EXTRA_PAUSE_DURATION_MS, 60 * 60 * 1000L) // 1 hour default
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                builder.addAction(0, "Pause 1h", pauseIntent)
            }
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "BlockAds VPN",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows VPN protection status"
                setShowBadge(false)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun wrapInIpUdp(
        originalPacket: ByteArray,
        originalLen: Int,
        originalIhl: Int,
        dnsPayload: ByteArray,
        isIpv4: Boolean,
    ): ByteArray? {
        val responseIhl = if (isIpv4) 20 else 40
        val udpLen = 8 + dnsPayload.size
        val ipLen = responseIhl + udpLen
        val packet = ByteArray(ipLen)

        if (isIpv4) {
            System.arraycopy(originalPacket, 0, packet, 0, 20)
            packet[0] = 0x45 // Version 4, IHL 5
            packet[2] = (ipLen shr 8).toByte()
            packet[3] = (ipLen and 0xFF).toByte()
            // Swap source and dest IP
            System.arraycopy(originalPacket, 12, packet, 16, 4)
            System.arraycopy(originalPacket, 16, packet, 12, 4)

            packet[10] = 0 // Clear checksum before calculation
            packet[11] = 0
            val ipChecksum = calculateIpv4Checksum(packet)
            packet[10] = (ipChecksum shr 8).toByte()
            packet[11] = (ipChecksum and 0xFF).toByte()
        } else {
            System.arraycopy(originalPacket, 0, packet, 0, 40)
            packet[4] = (udpLen shr 8).toByte()
            packet[5] = (udpLen and 0xFF).toByte()
            packet[6] = 17 // Next Header: UDP
            // Swap source and dest IPv6 addresses
            System.arraycopy(originalPacket, 8, packet, 24, 16)
            System.arraycopy(originalPacket, 24, packet, 8, 16)
        }

        // Swap ports using originalIhl
        packet[responseIhl] = originalPacket[originalIhl + 2]
        packet[responseIhl + 1] = originalPacket[originalIhl + 3]
        packet[responseIhl + 2] = originalPacket[originalIhl]
        packet[responseIhl + 3] = originalPacket[originalIhl + 1]

        packet[responseIhl + 4] = (udpLen shr 8).toByte()
        packet[responseIhl + 5] = (udpLen and 0xFF).toByte()
        packet[responseIhl + 6] = 0
        packet[responseIhl + 7] = 0

        System.arraycopy(dnsPayload, 0, packet, responseIhl + 8, dnsPayload.size)

        // Calculate UDP checksum
        val checksum = calculateUdpChecksum(packet, responseIhl, isIpv4)
        packet[responseIhl + 6] = (checksum shr 8).toByte()
        packet[responseIhl + 7] = (checksum and 0xFF).toByte()

        return packet
    }

    private fun calculateIpv4Checksum(packet: ByteArray): Int {
        var sum = 0L
        for (i in 0 until 20 step 2) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        }
        while ((sum shr 16) != 0L) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFFL).toInt()
    }

    private fun calculateUdpChecksum(
        packet: ByteArray,
        ihl: Int,
        isIpv4: Boolean,
    ): Int {
        val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
        var sum = 0L

        // Pseudo-header
        if (isIpv4) {
            // Source IP (4 bytes)
            sum += ((packet[12].toInt() and 0xFF) shl 8) or (packet[13].toInt() and 0xFF)
            sum += ((packet[14].toInt() and 0xFF) shl 8) or (packet[15].toInt() and 0xFF)
            // Dest IP (4 bytes)
            sum += ((packet[16].toInt() and 0xFF) shl 8) or (packet[17].toInt() and 0xFF)
            sum += ((packet[18].toInt() and 0xFF) shl 8) or (packet[19].toInt() and 0xFF)
            // Protocol (17)
            sum += 17
            // UDP Length
            sum += udpLen
        } else {
            // Source IPv6 (16 bytes)
            for (i in 0 until 8) {
                sum += ((packet[8 + i * 2].toInt() and 0xFF) shl 8) or (packet[9 + i * 2].toInt() and 0xFF)
            }
            // Dest IPv6 (16 bytes)
            for (i in 0 until 8) {
                sum += ((packet[24 + i * 2].toInt() and 0xFF) shl 8) or (packet[25 + i * 2].toInt() and 0xFF)
            }
            // UDP Length (4 bytes in pseudo-header for IPv6)
            sum += udpLen
            // Next Header (17)
            sum += 17
        }

        // UDP Header + Payload
        var i = ihl
        var remaining = udpLen
        // Ensure checksum field is 0 before calculation (already 0 from wrapInIpUdp, but for clarity)
        packet[ihl + 6] = 0
        packet[ihl + 7] = 0

        while (remaining > 1) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
            remaining -= 2
        }
        if (remaining > 0) {
            sum += (packet[i].toInt() and 0xFF) shl 8
        }

        while ((sum shr 16) != 0L) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }

        val finalSum = (sum.inv() and 0xFFFFL).toInt()
        return if (finalSum == 0 && !isIpv4) {
            0xFFFF
        } else if (finalSum == 0) {
            0
        } else {
            finalSum
        }
    }
}
