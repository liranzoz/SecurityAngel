package com.example.securityangel.ui.scanner

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.securityangel.R

data class ScanResult(val name: String, val status: String)


class ScanResultsAdapter(private val results: List<ScanResult>) :
    RecyclerView.Adapter<ScanResultsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvEngineName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val imgIcon: ImageView = view.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scan_result_sandbox, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        val context = holder.itemView.context

        holder.tvName.text = item.name
        holder.tvStatus.text = item.status.replaceFirstChar { it.uppercase() }

        if (item.status == "clean" || item.status == "unrated") {
            val greenColor = context.getColor(R.color.status_safe_text)
            holder.tvStatus.setTextColor(greenColor)
            holder.imgIcon.setColorFilter(greenColor)
            holder.imgIcon.setImageResource(R.drawable.ic_shield_check)
        } else {
            val redColor = context.getColor(R.color.status_unsafe_text)
            holder.tvStatus.setTextColor(redColor)
            holder.imgIcon.setColorFilter(redColor)
            holder.imgIcon.setImageResource(R.drawable.ic_warning)
        }
    }

    override fun getItemCount() = results.size
}