package com.example.securityangel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.securityangel.databinding.ActivityPasswordVaultBinding
import com.example.securityangel.databinding.ItemPasswordBinding
class PasswordVaultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordVaultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPasswordVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val accountsList = listOf(
            PasswordAccount("Google", "user@gmail.com", "google.com"),
            PasswordAccount("Netflix", "user@email.com", "netflix.com"),
            PasswordAccount("Amazon", "user@amazon.com", "amazon.com"),
            PasswordAccount("Facebook", "user.name", "facebook.com"),
            PasswordAccount("Twitter", "@username", "twitter.com")
        )

        val adapter = PasswordVaultAdapter(accountsList)
        binding.rvPasswords.layoutManager = LinearLayoutManager(this)
        binding.rvPasswords.adapter = adapter
    }
}
