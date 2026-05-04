package com.example.securityangel.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.securityangel.R
import com.example.securityangel.data.models.SecurityLog
import com.example.securityangel.databinding.ItemSecurityLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecurityLogAdapter(private var logs: List<SecurityLog>) :
    RecyclerView.Adapter<SecurityLogAdapter.LogViewHolder>() {

    inner class LogViewHolder(val binding: ItemSecurityLogBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemSecurityLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        val context = holder.itemView.context

        holder.binding.apply {
            tvTitle.text = getTitleByType(log.eventType)
            tvDescription.text = log.description

            val date = Date(log.timestamp)
            val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            tvTimestamp.text = format.format(date)

            when (log.eventType) {
                "LEAK_FOUND", "MALWARE_DETECTED" -> {
                    iconContainer.background.setTint(context.getColor(R.color.status_unsafe_bg))
                    ivLogIcon.setImageResource(android.R.drawable.stat_notify_error)
                    ivLogIcon.setColorFilter(context.getColor(R.color.status_unsafe_text))
                }
                "SCAN_SAFE" -> {
                    iconContainer.background.setTint(context.getColor(R.color.status_safe_bg))
                    ivLogIcon.setImageResource(R.drawable.ic_lock_check)
                    ivLogIcon.setColorFilter(context.getColor(R.color.status_safe_text))
                }
                "MEMBER_ADDED" -> {
                    iconContainer.background.setTint(context.getColor(R.color.icon_blue_bg))
                    ivLogIcon.setImageResource(android.R.drawable.ic_menu_add)
                    ivLogIcon.setColorFilter(context.getColor(R.color.icon_blue))
                }
                else -> {
                    iconContainer.background.setTint(context.getColor(R.color.background_main))
                    ivLogIcon.setImageResource(R.drawable.ic_info)
                    ivLogIcon.setColorFilter(context.getColor(R.color.text_secondary))
                }
            }
        }
    }

    private fun getTitleByType(type: String): String {
        return when (type) {
            "LEAK_FOUND" -> "Security Breach Detected"
            "SCAN_SAFE" -> "Scan Completed Safely"
            "MEMBER_ADDED" -> "Family Update"
            else -> "System Event"
        }
    }

    override fun getItemCount() = logs.size

    fun updateList(newLogs: List<SecurityLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
