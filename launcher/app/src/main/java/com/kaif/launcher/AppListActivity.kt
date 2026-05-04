package com.kaif.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    private var pickMode = false
    private var pickKey  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)
        pickMode = intent.getBooleanExtra("pickMode", false)
        pickKey  = intent.getStringExtra("pickKey") ?: ""

        val prefs     = getSharedPreferences("launcher", MODE_PRIVATE)
        val fontColor = Color.parseColor(prefs.getString("font_color", "#FFFFFF") ?: "#FFFFFF")

        findViewById<TextView>(R.id.appsLabel).setTextColor(fontColor)

        val recycler = findViewById<RecyclerView>(R.id.appRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < -30 && !rv.canScrollVertically(-1)) {
                    goHome()
                }
            }
        })

        recycler.adapter = AppAdapter(getInstalledApps(), fontColor) { pkg, label ->
            if (pkg == "com.kaif.launcher.SETTINGS") {
                startActivity(Intent(this, SettingsActivity::class.java))
                return@AppAdapter
            }
            if (pickMode) {
                prefs.edit().putString("${pickKey}_pkg", pkg)
                    .putString("${pickKey}_label", label).apply()
                finish()
            } else {
                packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            }
        }
    }

    private fun goHome() {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        goHome()
    }

    private fun getInstalledApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            .map { Pair(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .toMutableList()
        apps.add(0, Pair("com.kaif.launcher.SETTINGS", "X6Launcher Settings"))
        return apps
    }

    inner class AppAdapter(
        private val apps: List<Pair<String, String>>,
        private val fontColor: Int,
        private val onClick: (String, String) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.appName)
            val dot:  View     = view.findViewById(R.id.dot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (pkg, label) = apps[position]
            holder.name.text = label
            val color = if (pkg == "com.kaif.launcher.SETTINGS") getColor(R.color.accent) else fontColor
            holder.name.setTextColor(color)
            holder.dot.setBackgroundColor(color)
            holder.itemView.setOnClickListener { onClick(pkg, label) }
        }

        override fun getItemCount() = apps.size
    }
}
