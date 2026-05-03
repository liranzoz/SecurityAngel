package com.example.securityangel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SecurityAngelApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Set the night mode once, before any Activity's attachBaseContext() runs.
        // This is the only reliable place to do it: the Application is the single
        // entry point that always executes before any Activity lifecycle begins.
        val isDarkMode = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
