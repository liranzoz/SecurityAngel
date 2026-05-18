package com.example.securityangel.data.models

data class Family(
    val id: String = "",
    val adminId: String = "",
    val name: String = "",
    val members: List<String> = emptyList()
)
