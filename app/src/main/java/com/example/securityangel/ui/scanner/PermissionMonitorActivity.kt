package com.example.securityangel.ui.scanner

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.securityangel.databinding.ActivityPermissionMonitorBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.PermissionMonitor
import com.example.securityangel.utils.PermissionMonitor.AppPermissionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionMonitorActivity : BaseActivity() {

    private lateinit var binding: ActivityPermissionMonitorBinding
    private lateinit var adapter: AppPermissionAdapter
    private var allApps: List<AppPermissionInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionMonitorBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        setToolbarVisibility(false)

        setupRecyclerView()
        loadApps()
        buttonHandler()
    }

    override fun buttonHandler() {
        binding.btnBack.setOnClickListener { openDrawer() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = AppPermissionAdapter(
            items = emptyList(),
            onItemClick = { app -> showExplanationDialog(app) },
            onSettingsClick = { app -> PermissionMonitor.openAppPermissionSettings(this, app.packageName) }
        )
        binding.rvApps.adapter = adapter
    }

    private fun showExplanationDialog(app: AppPermissionInfo) {
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setMessage(app.riskExplanation)
            .setPositiveButton("Manage Permissions") { _, _ ->
                PermissionMonitor.openAppPermissionSettings(this, app.packageName)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                PermissionMonitor.getInstalledAppsWithPermissions(this@PermissionMonitorActivity)
            }
            allApps = apps
            binding.tvAppCount.text = "${apps.size} apps installed"
            updateRiskSummary(apps)
            showApps(apps)
        }
    }

    private fun updateRiskSummary(apps: List<AppPermissionInfo>) {
        val riskyCount = apps.count { it.riskLevel != PermissionMonitor.RiskLevel.SAFE }
        if (riskyCount > 0) {
            binding.cardRiskSummary.visibility = View.VISIBLE
            binding.tvRiskSummary.text =
                "$riskyCount app${if (riskyCount > 1) "s" else ""} with suspicious permissions detected"
        } else {
            binding.cardRiskSummary.visibility = View.GONE
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        showApps(filtered)
    }

    private fun showApps(apps: List<AppPermissionInfo>) {
        if (apps.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.cardApps.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.cardApps.visibility = View.VISIBLE
            adapter.updateList(apps)
        }
    }
}