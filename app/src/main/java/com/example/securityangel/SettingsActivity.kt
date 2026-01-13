package com.example.securityangel

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securityangel.databinding.ActivityFamilySafetyBinding
import com.example.securityangel.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContent(binding.root)

        setToolbarIconColor(isDarkBackground = false)
    }

    override fun buttonHandler() {
        TODO("Not yet implemented")
    }
}