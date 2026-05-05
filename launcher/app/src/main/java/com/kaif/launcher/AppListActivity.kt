package com.kaif.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    private var pickMode = false
    private var pickKey  = ""
    private lateinit var adapter: AppAdapter
    private val allApps = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)
        pickMode = intent.getBooleanExtra("pickMode", false)
        pickKey  = intent.getStringExtra("pickKey") ?: ""

        val prefs     = getSharedPreferences("launcher", MODE_PRIVATE)
        val fontColor = Color.parseColor(prefs.getString("font_color", "#FFFFFF") ?: "#FFFFFF")

        findViewById<TextView>(R.id.appsLabel).setTextColor(fontColor)

        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.setTextColor(fontColor)
        searchBar.setHintTextColor(Color.parseColor("#888888"))

        val recycler = findViewById<RecyclerView>(R.id.appRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        allApps.addAll(getInstalledApps())

        adapter = AppAdapter(allApps.toMutableList(), fontColor) { pkg, label ->
            if (pkg == "com.kaif.launcher.SETTINGS") {
                startActivity(Intent(this, SettingsActivity::class.java))
                return@AppAdapter
            }
            if (pickMode) {
                prefs.edit().putString("${pickKey}_pkg", pkg)
                    .putString("${pickKey}_label", label).apply()
                finish()
            } else {
                NotificationService.notifiedPackages.remove(pkg)
                adapter.notifyDataSetChanged()
                packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            }
        }
        recycler.adapter = adapter

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                val filtered = if (query.isEmpty()) allApps.toMutableList()
                else allApps.filter { (pkg, label) ->
                    pkg != "com.kaif.launcher.SETTINGS" && label.lowercase().contains(query)
                }.toMutableList()
                adapter.updateList(filtered)
            }
        })

        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val current = adapter.getCurrentList()
                if (current.isNotEmpty()) {
                    val (pkg, label) = current[0]
                    if (pkg != "com.kaif.launcher.SETTINGS") {
                        hideKeyboard()
                        if (pickMode) {
                            prefs.edit().putString("${pickKey}_pkg", pkg)
                                .putString("${pickKey}_label", label).apply()
                            finish()
                        } else {
                            NotificationService.notifiedPackages.remove(pkg)
                            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                        }
                    }
                }
                true
            } else false
        }

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < -30 && !rv.canScrollVertically(-1)) goHome()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun goHome() {
        hideKeyboard()
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    override fun onBackPressed() { goHome() }

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
        private var apps: MutableList<Pair<String, String>>,
        private val fontColor: Int,
        private val onClick: (String, String) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.appName)
            val dot:  View     = view.findViewById(R.id.dot)
            val notifDot: View = view.findViewById(R.id.notifDot)
        }

        fun updateList(newList: MutableList<Pair<String, String>>) {
            apps = newList
            notifyDataSetChanged()
        }

        fun getCurrentList() = apps

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (pkg, label) = apps[position]
            holder.name.text = label
            val color = if (pkg == "com.kaif.launcher.SETTINGS") getColor(R.color.accent) else fontColor
            holder.name.setTextColor(color)
            holder.dot.setBackgroundColor(color)
            val hasNotif = NotificationService.notifiedPackages.contains(pkg)
            holder.notifDot.visibility = if (hasNotif) View.VISIBLE else View.GONE
            holder.notifDot.setBackgroundColor(color)
            holder.itemView.setOnClickListener { onClick(pkg, label) }
        }

        override fun getItemCount() = apps.size
    }
}
