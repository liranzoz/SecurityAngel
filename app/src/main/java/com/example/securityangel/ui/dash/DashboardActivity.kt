package com.example.securityangel.ui.dash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.R
import com.example.securityangel.data.models.ScanHistoryItem
import com.example.securityangel.databinding.ActivityDashboardBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.ui.family.FamilySafetyActivity
import com.example.securityangel.ui.scanner.RecentScansAdapter
import com.example.securityangel.ui.vault.PasswordVaultActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = true)

        binding.rvRecentScans.layoutManager = LinearLayoutManager(this)

        loadRecentScans()
        listenToSecurityStatus()
        buttonHandler()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun listenToSecurityStatus() {
        if (userId == null) return

        db.collection("users").document(userId).addSnapshotListener { document, error ->
            if (error != null || document == null || !document.exists()) {
                return@addSnapshotListener
            }

            val activeRisks = document.get("activeRisks") as? List<String> ?: emptyList()
            val riskCount = activeRisks.size

            updateFamilyStatusUI(riskCount)
            calculateAndUpdateScore(riskCount)
        }
    }

    private fun updateFamilyStatusUI(riskCount: Int) {
        if (riskCount == 0) {
            binding.tvFamilyStatus.text = "Safe"
            binding.tvFamilyStatus.setTextColor(getColor(R.color.primary_green))
            binding.lottieFamily.speed = 0.5f
        } else {
            binding.tvFamilyStatus.text = "$riskCount Alert(s)"
            binding.tvFamilyStatus.setTextColor(getColor(R.color.status_warning_text_red))
            binding.lottieFamily.speed = 1.2f
        }
    }

    private fun calculateAndUpdateScore(riskCount: Int) {
        var score = 100 - (riskCount * 15)
        if (score < 20) score = 20

        binding.tvScoreValue.text = score.toString()

        val progressBar = binding.progressBarScore

        if (score < 70) {
            progressBar.progressDrawable.setTint(getColor(R.color.status_warning_text_red))
        }

        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, score)
        animation.duration = 1000
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    override fun buttonHandler() {
        val btnVault = binding.lyVault
        btnVault.setOnClickListener {
            val intent = Intent(this, PasswordVaultActivity::class.java)
            startActivity(intent)
        }

        val btnFamilySafety = binding.lyFamilySafety
        btnFamilySafety.setOnClickListener {
            val intent = Intent(this, FamilySafetyActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadRecentScans() {
        if (userId == null) return

        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("scans")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                val scansList = documents.toObjects(ScanHistoryItem::class.java)

                if (scansList.isNotEmpty()) {
                    binding.rvRecentScans.adapter = RecentScansAdapter(scansList)
                    binding.rvRecentScans.visibility = View.VISIBLE
                } else {
                    binding.rvRecentScans.visibility = View.GONE
                }
            }
    }
}