package com.example.securityangel.ui.ai

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.securityangel.R
import com.example.securityangel.data.models.ChatMessage
import com.example.securityangel.databinding.ItemChatMessageBinding

class ChatAdapter(private val messages: MutableList<ChatMessage> = mutableListOf()) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        val context = holder.itemView.context

        if (msg.isLoading) {
            holder.binding.cardMessage.visibility = View.GONE
            holder.binding.lottieLoading.visibility = View.VISIBLE
            holder.binding.rootLayout.gravity = Gravity.START

        } else {
            holder.binding.cardMessage.visibility = View.VISIBLE
            holder.binding.lottieLoading.visibility = View.GONE
            holder.binding.tvMessage.text = msg.text

            if (msg.isUser) {
                holder.binding.rootLayout.gravity = Gravity.END
                holder.binding.cardMessage.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_green))
                holder.binding.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                holder.binding.rootLayout.gravity = Gravity.START
                holder.binding.cardMessage.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                holder.binding.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
    }

    override fun getItemCount() = messages.size

    fun removeLoadingMessage() {
        val loadingIndex = messages.indexOfFirst { it.isLoading }
        if (loadingIndex != -1) {
            messages.removeAt(loadingIndex)
            notifyItemRemoved(loadingIndex)
        }
    }
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}