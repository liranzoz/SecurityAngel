package com.example.securityangel

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.securityangel.databinding.ItemPasswordBinding

class PasswordVaultAdapter(private val accounts: List<PasswordAccount>) :
    RecyclerView.Adapter<PasswordVaultAdapter.PasswordViewHolder>() {

    inner class PasswordViewHolder(val binding: ItemPasswordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PasswordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        val account = accounts[position]
        val context = holder.itemView.context

        holder.binding.tvSiteName.text = account.siteName
        holder.binding.tvUserEmail.text = account.email

        val logoUrl = "https://logo.clearbit.com/${account.domain}"

        Glide.with(context)
            .load(logoUrl)
            .circleCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(holder.binding.ivSiteIcon)

        holder.binding.btnCopy.setOnClickListener {
            context.toast("Copied password for ${account.siteName}")
        }

        holder.binding.btnView.setOnClickListener {
            context.toast("Viewing ${account.siteName}")
        }
    }

    override fun getItemCount() = accounts.size
}