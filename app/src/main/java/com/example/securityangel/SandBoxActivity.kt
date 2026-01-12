package com.example.securityangel

import ScanResult
import ScanResultsAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securityangel.databinding.ActivityFamilySafetyBinding
import com.example.securityangel.databinding.ActivitySandBoxBinding

class SandBoxActivity : BaseActivity(){

    private lateinit var binding: ActivitySandBoxBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sand_box)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        binding = ActivitySandBoxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buttonHandler()

        val dummyData = listOf(
            ScanResult("Norton", "Clean"),
            ScanResult("McAfee", "Clean"),
            ScanResult("Kaspersky", "Clean"),
            ScanResult("Avast", "Clean"),
            ScanResult("Bitdefender", "Clean"),
            ScanResult("ESET", "Clean"),
            ScanResult("Malwarebytes", "Clean")
        )

        val recyclerView = findViewById<RecyclerView>(R.id.rvResults)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ScanResultsAdapter(dummyData)
    }

    override fun buttonHandler() {
        val btnArrowBack = binding.imgBackClickable
        btnArrowBack.isClickable = true
        btnArrowBack.isFocusable = true
        btnArrowBack.setOnClickListener { finish() }
    }


}


