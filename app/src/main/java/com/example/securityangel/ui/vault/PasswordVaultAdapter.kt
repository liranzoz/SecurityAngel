package com.example.securityangel.ui.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.securityangel.R
import com.example.securityangel.data.models.PasswordAccount
import com.example.securityangel.databinding.ItemPasswordBinding
import com.example.securityangel.utils.toast

class PasswordVaultAdapter(private var accounts: List<PasswordAccount>, private val onItemAction: (PasswordAccount, String) -> Unit) :
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

        holder.binding.apply {
            holder.binding.tvSiteName.text = account.siteName
            holder.binding.tvUserEmail.text = account.email

            if (account.isPasswordVisible) {
                holder.binding.tvPasswordHidden.text = account.password
                holder.binding.tvPasswordHidden.setTextColor(context.getColor(R.color.text_primary))
                holder.binding.btnView.setImageResource(R.drawable.ic_eye)
            } else {
                holder.binding.tvPasswordHidden.text = "••••••••"
                holder.binding.tvPasswordHidden.setTextColor(Color.parseColor("#BDBDBD"))
                holder.binding.btnView.setImageResource(R.drawable.ic_eye)
            }

            val logoUrl = "https://www.google.com/s2/favicons?domain=${account.domain}&sz=128"
            Glide.with(context)
                .load(logoUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.binding.ivSiteIcon)

            if (account.isLeaked) {
                ivLeakedAlert.visibility = View.VISIBLE
                root.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.status_unsafe_bg
                    )
                )
            } else {
                ivLeakedAlert.visibility = View.GONE
                root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }
        }

        holder.binding.btnCopy.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Password", account.password)
            clipboard.setPrimaryClip(clip)

            context.toast("Password copied!")
        }

        holder.binding.btnView.setOnClickListener {
            account.isPasswordVisible = !account.isPasswordVisible
            notifyItemChanged(position)
        }

        holder.itemView.setOnLongClickListener {
            onItemAction(account, "options")
            true
        }
    }

    fun updateList(newList: List<PasswordAccount>) {
        accounts = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = accounts.size
}