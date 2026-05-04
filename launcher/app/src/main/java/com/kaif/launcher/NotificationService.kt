package com.kaif.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        startForegroundIfNeeded()
    }

    private fun startForegroundIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "x6launcher_service"
            val channel = NotificationChannel(
                channelId,
                "X6Launcher Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setContentTitle("X6Launcher")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            startForeground(1, notification)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { notifiedPackages.add(it.packageName) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            val remaining = activeNotifications?.any { n -> n.packageName == it.packageName } ?: false
            if (!remaining) notifiedPackages.remove(it.packageName)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        notifiedPackages.clear()
        activeNotifications?.forEach { notifiedPackages.add(it.packageName) }
        // Notify MainActivity that service is connected
        val intent = Intent("com.kaif.launcher.SERVICE_CONNECTED")
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    companion object {
        val notifiedPackages = mutableSetOf<String>()
        var isConnected = false
    }
}
