package com.example.securityangel.data.models

data class PasswordAccount (
    var id: String = "",
    val searchKey : String = "",
    val siteName: String = "",
    val email: String = "",
    val domain: String = "",
    val password: String = "",
    var isPasswordVisible: Boolean = false,
    var isLeaked: Boolean = false
)
