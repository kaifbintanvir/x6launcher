package com.kaif.launcher

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        updateLabels()

        // Opens system wallpaper picker directly
        findViewById<Button>(R.id.btnSetWallpaper).setOnClickListener {
            try {
                val intent = WallpaperManager.getInstance(this).getCropAndSetWallpaperIntent(
                    android.net.Uri.EMPTY
                )
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to generic system wallpaper chooser
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                startActivity(Intent.createChooser(intent, "Set Wallpaper"))
            }
        }

        val colorMap = mapOf(
            R.id.colorWhite  to "#FFFFFF",
            R.id.colorAccent to "#00FFB2",
            R.id.colorGold   to "#FFD700",
            R.id.colorCyan   to "#00CFFF",
            R.id.colorRose   to "#FF6B9D",
            R.id.colorOrange to "#FF8C00"
        )
        colorMap.forEach { (btnId, hex) ->
            findViewById<Button>(btnId).setOnClickListener {
                getSharedPreferences("launcher", MODE_PRIVATE).edit()
                    .putString("font_color", hex).apply()
                sendBroadcast(Intent("com.kaif.launcher.REFRESH"))
            }
        }

        listOf("clock", "calendar", "left", "right", "doubletap").forEach { key ->
            val btnId = when(key) {
                "clock"    -> R.id.btnSetClock
                "calendar" -> R.id.btnSetCalendar
                "left"     -> R.id.btnSetLeft
                "right"    -> R.id.btnSetRight
                else       -> R.id.btnSetDoubleTap
            }
            findViewById<Button>(btnId).setOnClickListener {
                startActivity(Intent(this, AppListActivity::class.java).apply {
                    putExtra("pickMode", true)
                    putExtra("pickKey", key)
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateLabels()
    }

    private fun updateLabels() {
        val prefs = getSharedPreferences("launcher", MODE_PRIVATE)
        findViewById<TextView>(R.id.clockAppName).text = prefs.getString("clock_label", "Not set")
        findViewById<TextView>(R.id.calendarAppName).text = prefs.getString("calendar_label", "Not set")
        findViewById<TextView>(R.id.leftAppName).text = prefs.getString("left_label", "Not set")
        findViewById<TextView>(R.id.rightAppName).text = prefs.getString("right_label", "Not set")
        findViewById<TextView>(R.id.doubleTapAppName).text = prefs.getString("doubletap_label", "Not set")
        findViewById<TextView>(R.id.wallpaperName).text = "Managed by system"
    }
}
