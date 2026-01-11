package com.example.securityangel

import android.content.Intent
import android.os.Bundle
import com.example.securityangel.databinding.ActivityDashboardBinding

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContent(binding.root)

        setToolbarIconColor(isDarkBackground = true)

//        val btnVault = binding.lyVault
//        btnVault.setOnClickListener {
//            val intent = intent(this, )
//
//        }

        val btnFamilySafety = binding.lyFamilySafety
        btnFamilySafety.setOnClickListener {
            val intent = Intent(this, FamilySafetyActivity::class.java)
            startActivity(intent)
        }
    }
}