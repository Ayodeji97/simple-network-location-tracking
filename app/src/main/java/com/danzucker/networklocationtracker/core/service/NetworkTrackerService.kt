package com.danzucker.networklocationtracker.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.danzucker.networklocationtracker.R
import com.danzucker.networklocationtracker.core.data.networktracker.NetworkWithLocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject

class NetworkTrackerService : Service() {

    private val tracker: NetworkWithLocationTracker by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trackingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> start()
        }
        return START_STICKY
    }

    private fun start() {
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (trackingJob == null || trackingJob?.isActive != true) {
            trackingJob = tracker.startTracking(scope)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.tracking_channel_description)
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.tracking_notification_title))
        .setContentText(getString(R.string.tracking_notification_text))
        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // placeholder mipmap-free icon
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .addAction(
            0,
            getString(R.string.tracking_notification_stop),
            stopPendingIntent(),
        )
        .build()

    private fun stopPendingIntent(): PendingIntent {
        val intent = Intent(this, NetworkTrackerService::class.java).apply { action = ACTION_STOP }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(this, STOP_REQUEST_CODE, intent, flags)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "network_tracker_channel"
        private const val NOTIFICATION_ID = 1
        private const val STOP_REQUEST_CODE = 101

        const val ACTION_START = "com.danzucker.networklocationtracker.action.START"
        const val ACTION_STOP = "com.danzucker.networklocationtracker.action.STOP"

        fun startIntent(context: Context): Intent =
            Intent(context, NetworkTrackerService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context): Intent =
            Intent(context, NetworkTrackerService::class.java).apply { action = ACTION_STOP }
    }
}
