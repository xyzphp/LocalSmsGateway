package com.example.smsgateway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.PowerManager

class SmsGatewayService : Service() {
    private lateinit var repository: GatewayRepository
    private var webServer: LocalWebServer? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private val inboxSyncHandler = Handler(Looper.getMainLooper())
    private val inboxSyncRunnable = object : Runnable {
        override fun run() {
            SmsInboxSync.syncAsync(this@SmsGatewayService)
            inboxSyncHandler.postDelayed(this, INBOX_SYNC_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = GatewayRepository(this)
        createNotificationChannel()
        startForegroundCompat()
        try {
            webServer = LocalWebServer(this).also { it.start() }
            isActive = true
            // Remember an intentional start so the gateway can be restored after a reboot.
            repository.setAutoStart(true)
            acquireWifiLock()
            acquireCpuWakeLock()
            reconcileStaleSends()
            startInboxSync()
        } catch (_: Exception) {
            isActive = false
            repository.setServiceRunning(false)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // The activity can be dismissed from Recents while the gateway should keep
        // serving the LAN. Re-submit the service only after an intentional start;
        // the Stop button clears autoStart before stopping the service.
        if (!repository.settings().autoStart) return
        try {
            val serviceIntent = Intent(applicationContext, SmsGatewayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (_: Exception) {
            // START_STICKY remains the normal recovery path if the OEM rejects this call.
        }
    }

    override fun onDestroy() {
        isActive = false
        inboxSyncHandler.removeCallbacks(inboxSyncRunnable)
        releaseCpuWakeLock()
        releaseWifiLock()
        webServer?.stop()
        repository.setServiceRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun reconcileStaleSends() {
        repository.reconcileStaleSending().forEach { record ->
            WebhookDispatcher.enqueue(this, "sms.failed", record.toJson())
        }
    }

    private fun startInboxSync() {
        inboxSyncHandler.removeCallbacks(inboxSyncRunnable)
        inboxSyncHandler.post(inboxSyncRunnable)
    }

    @Suppress("DEPRECATION")
    private fun acquireWifiLock() {
        if (wifiLock?.isHeld == true) return
        val wifiManager = getSystemService(WifiManager::class.java) ?: return
        try {
            // Low-latency mode is optimized for foreground traffic and may still let
            // the Wi-Fi chipset suspend in Doze. The gateway needs the high-performance
            // lock because its HTTP listener must remain reachable with the screen off.
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "SmsGateway:LanServer"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
            wifiLock = null
        }
    }

    private fun releaseWifiLock() {
        try {
            wifiLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {
            // The system may already have released the lock while the service is stopping.
        }
        wifiLock = null
    }

    private fun acquireCpuWakeLock() {
        if (cpuWakeLock?.isHeld == true) return
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        try {
            cpuWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmsGateway:BackgroundServer"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
            cpuWakeLock = null
        }
    }

    private fun releaseCpuWakeLock() {
        try {
            cpuWakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {
            // The system may already have released the lock while the service is stopping.
        }
        cpuWakeLock = null
    }

    private fun startForegroundCompat() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(getString(com.example.smsgateway.R.string.app_name))
            .setContentText("服务运行中 · 同一 Wi-Fi 可访问")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(com.example.smsgateway.R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(com.example.smsgateway.R.string.service_channel_description)
            }
        )
    }

    companion object {
        @Volatile
        var isActive: Boolean = false

        const val ACTION_STOP = "com.example.smsgateway.action.STOP"
        private const val CHANNEL_ID = "sms_gateway_service"
        private const val NOTIFICATION_ID = 1001
        private const val INBOX_SYNC_INTERVAL_MILLIS = 5_000L
    }
}
