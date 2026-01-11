package com.example.securityangel

import android.os.Bundle
import com.example.securityangel.databinding.ActivityDashboardBinding

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContent(binding.root)

        setToolbarIconColor(isDarkBackground = true)
    }
}