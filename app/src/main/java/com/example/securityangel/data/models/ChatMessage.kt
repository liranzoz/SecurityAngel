package com.example.securityangel.data.models

import com.google.firebase.firestore.PropertyName

data class ChatMessage(
    val text: String = "",
    @get:PropertyName("isUser")
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isLoading")
    val isLoading : Boolean = false
)