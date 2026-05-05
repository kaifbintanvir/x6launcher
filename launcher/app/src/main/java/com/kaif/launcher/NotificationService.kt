package com.kaif.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.session.MediaSessionManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {

    private var mediaCallback: MediaSessionManager.OnActiveSessionsChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundIfNeeded()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        notifiedPackages.clear()
        activeNotifications?.forEach { notifiedPackages.add(it.packageName) }

        // Listen for media session changes
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = android.content.ComponentName(this, NotificationService::class.java)
            mediaCallback = MediaSessionManager.OnActiveSessionsChangedListener {
                sendBroadcast(Intent("com.kaif.launcher.MEDIA_CHANGED"))
            }
            msm.addOnActiveSessionsChangedListener(mediaCallback!!, component)
        } catch (e: Exception) { }

        sendBroadcast(Intent("com.kaif.launcher.MEDIA_CHANGED"))
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            mediaCallback?.let { msm.removeOnActiveSessionsChangedListener(it) }
        } catch (e: Exception) { }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { notifiedPackages.add(it.packageName) }
        sendBroadcast(Intent("com.kaif.launcher.MEDIA_CHANGED"))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            val remaining = activeNotifications?.any { n -> n.packageName == it.packageName } ?: false
            if (!remaining) notifiedPackages.remove(it.packageName)
        }
    }

    private fun startForegroundIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "x6launcher_service"
            val channel = NotificationChannel(channelId, "X6Launcher Service",
                NotificationManager.IMPORTANCE_MIN).apply {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    companion object {
        val notifiedPackages = mutableSetOf<String>()
    }
}
