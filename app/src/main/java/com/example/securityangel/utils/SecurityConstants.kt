package com.example.securityangel.utils

object SecurityConstants {
    const val RISK_PASSWORD_LEAK = "risk_password_leak" // סיסמה דלפה (מהוולט)
    const val RISK_MALICIOUS_APP = "risk_malicious_app" // אפליקציה מסוכנת
    const val RISK_UNSECURE_WIFI = "risk_unsecure_wifi" // ווי-פיי ציבורי
    const val RISK_OUTDATED_OS = "risk_outdated_os"     // מערכת הפעלה ישנה

    const val STATUS_SAFE = "safe"
    const val STATUS_AT_RISK = "at_risk"
}