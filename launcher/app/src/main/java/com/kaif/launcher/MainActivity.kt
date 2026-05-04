package com.kaif.launcher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var leftHint: TextView
    private lateinit var rightHint: TextView
    private lateinit var accentLine: View
    private lateinit var musicWidget: LinearLayout
    private lateinit var musicTitle: TextView
    private lateinit var musicArtist: TextView
    private lateinit var btnPlayPause: TextView
    private lateinit var btnPrev: TextView
    private lateinit var btnNext: TextView
    private lateinit var media: MediaController

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applyAppearance()
        }
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            updateMusic()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockView    = findViewById(R.id.clockView)
        dateView     = findViewById(R.id.dateView)
        leftHint     = findViewById(R.id.leftAppHint)
        rightHint    = findViewById(R.id.rightAppHint)
        accentLine   = findViewById(R.id.accentLine)
        musicWidget  = findViewById(R.id.musicWidget)
        musicTitle   = findViewById(R.id.musicTitle)
        musicArtist  = findViewById(R.id.musicArtist)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrev      = findViewById(R.id.btnPrev)
        btnNext      = findViewById(R.id.btnNext)

        media = MediaController(this)

        clockView.setOnClickListener { launchAssignedApp("clock") }
        dateView.setOnClickListener  { launchAssignedApp("calendar") }
        btnPlayPause.setOnClickListener { media.playPause(); updateMusic() }
        btnPrev.setOnClickListener      { media.prev();      updateMusic() }
        btnNext.setOnClickListener      { media.next();      updateMusic() }

        val gestureView = findViewById<GestureView>(R.id.gestureView)
        gestureView.onSwipeUp = {
            startActivity(Intent(this, AppListActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        gestureView.onSwipeDown = {
            try {
                val sb = Class.forName("android.app.StatusBarManager")
                val m  = sb.getMethod("expandNotificationsPanel")
                m.isAccessible = true
                m.invoke(getSystemService("statusbar"))
            } catch (e: Exception) { }
        }
        gestureView.onSwipeLeft  = { launchAssignedApp("left") }
        gestureView.onSwipeRight = { launchAssignedApp("right") }
        gestureView.onDoubleTap  = { launchAssignedApp("doubletap") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, IntentFilter("com.kaif.launcher.REFRESH"),
                Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(refreshReceiver, IntentFilter("com.kaif.launcher.REFRESH"))
        }

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        val asked = prefs.getBoolean("notif_asked", false)
        if (asked) return
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val granted = enabled?.contains(packageName) == true
        if (!granted) {
            AlertDialog.Builder(this)
                .setTitle("Enable Notifications & Music")
                .setMessage("Grant notification access for app dots and music widget controls?")
                .setPositiveButton("Enable") { _, _ ->
                    prefs.edit().putBoolean("notif_asked", true).apply()
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("Skip") { _, _ ->
                    prefs.edit().putBoolean("notif_asked", true).apply()
                }
                .show()
        }
    }

    override fun onBackPressed() { }

    override fun onResume() {
        super.onResume()
        handler.post(clockRunnable)
        applyAppearance()
        updateHints()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(refreshReceiver) } catch (e: Exception) { }
    }

    private fun updateMusic() {
        val info = try { media.getInfo() } catch (e: Exception) { null }
        if (info != null) {
            musicWidget.visibility = View.VISIBLE
            musicTitle.text   = info.title
            musicArtist.text  = info.artist
            btnPlayPause.text = if (info.isPlaying) "⏸" else "▶"
            musicTitle.isSelected = true
        } else {
            musicWidget.visibility = View.GONE
        }
    }

    private fun applyAppearance() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        val fontColor = Color.parseColor(prefs.getString("font_color", "#FFFFFF") ?: "#FFFFFF")
        dateView.setTextColor(fontColor)
        leftHint.setTextColor(fontColor)
        rightHint.setTextColor(fontColor)
        accentLine.setBackgroundColor(fontColor)
        musicTitle.setTextColor(fontColor)
        btnPlayPause.setTextColor(fontColor)
        btnPrev.setTextColor(fontColor)
        btnNext.setTextColor(fontColor)
        updateHints()
    }

    private fun launchAssignedApp(key: String) {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        val pkg = prefs.getString("${key}_pkg", null)
        if (pkg != null) {
            packageManager.getLaunchIntentForPackage(pkg)?.let {
                startActivity(it); return
            }
        }
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun updateHints() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        leftHint.text  = "← ${prefs.getString("left_label",  "SET APP")!!.uppercase()}"
        rightHint.text = "${prefs.getString("right_label", "SET APP")!!.uppercase()} →"
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        clockView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        dateView.text  = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
            .format(now.time).uppercase()
    }
}
