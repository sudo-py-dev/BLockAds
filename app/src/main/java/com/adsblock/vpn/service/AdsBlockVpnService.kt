package com.adsblock.vpn.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.adsblock.vpn.R
import com.adsblock.vpn.data.DnsProviders
import com.adsblock.vpn.data.SettingsRepository
import com.adsblock.vpn.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdsBlockVpnService : VpnService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    var dnsTunnel: DnsTunnel? = null
    private var pauseJob: Job? = null

    companion object {
        const val ACTION_START = "com.adsblock.vpn.START"
        const val ACTION_STOP = "com.adsblock.vpn.STOP"
        const val ACTION_PAUSE = "com.adsblock.vpn.PAUSE"
        const val ACTION_RESUME = "com.adsblock.vpn.RESUME"
        const val EXTRA_PAUSE_DURATION_MINS = "extra_pause_duration_mins"
        private const val NOTIFICATION_CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent == null) return START_STICKY

        // Basic security: Ensure the intent is intended for our package
        val callingPackage = intent.`package` ?: intent.component?.packageName
        if (callingPackage != null && callingPackage != packageName) {
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            ACTION_PAUSE -> {
                val duration = intent.getLongExtra(EXTRA_PAUSE_DURATION_MINS, 15L).coerceIn(1, 1440)
                pauseVpn(duration)
            }
            ACTION_RESUME -> resumeVpn()
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val isPaused = dnsTunnel?.isPaused == true
        val isFallback = dnsTunnel?.isFallbackActive == true
        VpnStateManager.updateState(if (isPaused) VpnState.PAUSED else VpnState.CONNECTED)

        val title: String
        val text: String
        val icon: Int

        when {
            isPaused -> {
                title = getString(R.string.notification_paused_title)
                text = getString(R.string.notification_paused_text)
                icon = android.R.drawable.ic_secure
            }
            isFallback -> {
                title = getString(R.string.notification_fallback_title)
                text = getString(R.string.notification_fallback_text)
                icon = android.R.drawable.stat_notify_error
            }
            else -> {
                title = getString(R.string.notification_vpn_title)
                text = getString(R.string.notification_vpn_text)
                icon = android.R.drawable.ic_secure
            }
        }

        val builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setOngoing(true)

        if (isPaused) {
            val resumeIntent = Intent(this, AdsBlockVpnService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent =
                PendingIntent.getService(
                    this,
                    0,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder.addAction(android.R.drawable.ic_media_play, getString(R.string.btn_resume), resumePendingIntent)
        } else {
            val pauseIntent =
                Intent(this, AdsBlockVpnService::class.java).apply {
                    action = ACTION_PAUSE
                    putExtra(EXTRA_PAUSE_DURATION_MINS, 15L)
                }
            val pausePendingIntent =
                PendingIntent.getService(
                    this,
                    1,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder.addAction(android.R.drawable.ic_media_pause, getString(R.string.btn_pause), pausePendingIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    private fun handleFallbackStateChange(isFallback: Boolean) {
        updateNotification()
    }

    private fun pauseVpn(durationMins: Long) {
        dnsTunnel?.isPaused = true
        updateNotification()

        pauseJob?.cancel()
        if (durationMins > 0) {
            pauseJob =
                serviceScope.launch {
                    delay(durationMins * 60 * 1000)
                    resumeVpn()
                }
        }
    }

    private fun resumeVpn() {
        pauseJob?.cancel()
        dnsTunnel?.isPaused = false
        updateNotification()
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        serviceScope.launch {
            try {
                val settings = SettingsRepository(applicationContext)
                val providerId = settings.dnsProviderId.first()
                val protocol = settings.dnsProtocol.first()
                
                val provider = DnsProviders.getProviderById(providerId)
                val upstreamEndpoint = if (protocol == "dot") provider?.dotIp ?: "94.140.14.14" else provider?.dohUrl ?: "https://dns.adguard-dns.com/dns-query"

                val builder = Builder()
                builder.setSession(getString(R.string.app_name))
                builder.addAddress("10.0.0.2", 32)

                val virtualDnsIp = "10.0.0.1"
                builder.addDnsServer(virtualDnsIp)
                builder.addRoute(virtualDnsIp, 32)
                builder.allowBypass()

                vpnInterface = builder.establish()

                vpnInterface?.let {
                    dnsTunnel = DnsTunnel(this@AdsBlockVpnService, it.fileDescriptor, upstreamEndpoint, protocol, serviceScope) { isFallback ->
                        handleFallbackStateChange(isFallback)
                    }
                    dnsTunnel?.start()
                    updateNotification()
                }
            } catch (e: Exception) {
                Logger.e("AdsBlockVpnService", "Error starting VPN", e)
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        VpnStateManager.updateState(VpnState.DISCONNECTED)
        pauseJob?.cancel()
        dnsTunnel?.stop()
        dnsTunnel = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Logger.e("AdsBlockVpnService", "Error closing VPN interface", e)
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
