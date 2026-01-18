package com.example.securityangel.ui.scanner

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.securityangel.R
import com.example.securityangel.data.models.ScanHistoryItem

class RecentScansAdapter(private val scans: List<ScanHistoryItem>) :
    RecyclerView.Adapter<RecentScansAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrl: TextView = view.findViewById(R.id.tvTitle) // וודא שה-ID תואם ל-XML שלך
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = scans[position]

        holder.tvUrl.text = item.url

        val timeAgo = DateUtils.getRelativeTimeSpanString(item.timestamp)
        holder.tvTime.text = timeAgo

        if (item.status == "safe") {
            holder.tvStatus.text = "Safe"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.primary_green))
            holder.tvStatus.background.setTint(holder.itemView.context.getColor(R.color.status_safe_bg))
            holder.imgIcon.setImageResource(R.drawable.ic_shield_check)
        } else {
            holder.tvStatus.text = "Unsafe"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.status_unsafe_text))
            holder.tvStatus.background.setTint(holder.itemView.context.getColor(R.color.status_unsafe_bg))
            holder.imgIcon.setImageResource(R.drawable.ic_warning)
        }
    }

    override fun getItemCount() = scans.size
}