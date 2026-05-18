package com.example.securityangel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SecurityAngelApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val isDarkMode = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
