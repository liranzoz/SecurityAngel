package com.example.securityangel.data.models

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = "",

    val familyId: String? = null,
    val securityStatus: String = "Safe",
    val riskCount: Int = 0,
    val lastScanDate: Long = 0,
    val securityScore: Int = 100,
    val activeRisks: List<String> = emptyList()
)
