package com.example.securityangel.data.models

data class SecurityLog(
    val id: String = "",
    val familyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventType: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)