package com.example.securityangel.ui.scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.securityangel.R
import com.example.securityangel.databinding.ItemAppPermissionBinding
import com.example.securityangel.utils.PermissionMonitor.AppPermissionInfo
import com.example.securityangel.utils.PermissionMonitor.RiskLevel

class AppPermissionAdapter(
    private var items: List<AppPermissionInfo>,
    private val onSettingsClick: (AppPermissionInfo) -> Unit,
    private val onItemClick: (AppPermissionInfo) -> Unit
) : RecyclerView.Adapter<AppPermissionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppPermissionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppPermissionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.binding.apply {
            tvAppName.text = item.appName
            tvPackageName.text = item.packageName

            // Risk pill
            val (pillBg, pillText, pillLabel) = when (item.riskLevel) {
                RiskLevel.HIGH   -> Triple(R.color.status_unsafe_bg,  R.color.status_unsafe_text,      "HIGH")
                RiskLevel.MEDIUM -> Triple(R.color.status_warning_bg, R.color.status_warning_text_red, "MEDIUM")
                RiskLevel.LOW    -> Triple(R.color.status_warning_bg, R.color.status_warning_text,     "LOW")
                RiskLevel.SAFE   -> Triple(R.color.status_safe_bg,    R.color.status_safe_text,        "SAFE")
            }
            tvPermissionCount.text = pillLabel
            tvPermissionCount.setTextColor(ContextCompat.getColor(context, pillText))
            tvPermissionCount.backgroundTintList = ContextCompat.getColorStateList(context, pillBg)

            // Third line: OCP summary for risky apps, "Tap for details" hint for trusted
            when {
                item.riskLevel != RiskLevel.SAFE && item.sensitivePermissionsSummary.isNotEmpty() -> {
                    tvSensitivePerms.visibility = View.VISIBLE
                    tvSensitivePerms.text = item.sensitivePermissionsSummary
                    val noteColor = if (item.riskLevel == RiskLevel.HIGH) R.color.status_unsafe_text
                                    else R.color.status_warning_text
                    tvSensitivePerms.setTextColor(ContextCompat.getColor(context, noteColor))
                }
                item.riskLevel != RiskLevel.SAFE -> {
                    tvSensitivePerms.visibility = View.VISIBLE
                    tvSensitivePerms.text = "Tap for details"
                    tvSensitivePerms.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                else -> tvSensitivePerms.visibility = View.GONE
            }

            // Load real app icon
            try {
                val icon = context.packageManager.getApplicationIcon(item.packageName)
                ivAppIcon.setImageDrawable(icon)
                ivAppIcon.background = null
                ivAppIcon.setPadding(0, 0, 0, 0)
            } catch (_: Exception) { }

            // Tap row → show explanation dialog; tap gear → open system settings
            root.setOnClickListener { onItemClick(item) }
            btnOpenSettings.setOnClickListener { onSettingsClick(item) }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<AppPermissionInfo>) {
        items = newItems
        notifyDataSetChanged()
    }
}
