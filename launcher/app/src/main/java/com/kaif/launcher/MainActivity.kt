package com.kaif.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
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

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applyAppearance()
            updateClock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockView  = findViewById(R.id.clockView)
        dateView   = findViewById(R.id.dateView)
        leftHint   = findViewById(R.id.leftAppHint)
        rightHint  = findViewById(R.id.rightAppHint)
        accentLine = findViewById(R.id.accentLine)

        clockView.setOnClickListener { launchAssignedApp("clock") }
        dateView.setOnClickListener  { launchAssignedApp("calendar") }

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

    private fun applyAppearance() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        val fontColor = Color.parseColor(prefs.getString("font_color", "#FFFFFF") ?: "#FFFFFF")
        dateView.setTextColor(fontColor)
        leftHint.setTextColor(fontColor)
        rightHint.setTextColor(fontColor)
        accentLine.setBackgroundColor(fontColor)
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
        leftHint.text  = "← ${prefs.getString("left_label", "SET APP")!!.uppercase()}"
        rightHint.text = "${prefs.getString("right_label", "SET APP")!!.uppercase()} →"
    }

    private fun updateClock() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        val use24h = prefs.getBoolean("use_24h", true)
        val now = Calendar.getInstance()
        // 12h format without AM/PM — just the time
        val timeFormat = if (use24h) "HH:mm" else "hh:mm"
        clockView.text = SimpleDateFormat(timeFormat, Locale.getDefault()).format(now.time)
        dateView.text  = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
            .format(now.time).uppercase()
    }
}
