package com.example.securityangel.ui.dash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.ui.family.FamilySafetyActivity
import com.example.securityangel.ui.vault.PasswordVaultActivity
import com.example.securityangel.ui.scanner.RecentScansAdapter
import com.example.securityangel.data.models.ScanHistoryItem
import com.example.securityangel.databinding.ActivityDashboardBinding
import com.example.securityangel.ui.base.BaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = true)

        binding.rvRecentScans.layoutManager = LinearLayoutManager(this)
        loadRecentScans()
        buttonHandler()
    }

    override fun onResume() {
        super.onResume()
        loadRecentScans()
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

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
                    // אופציונלי: להסתיר את הרשימה אם אין סריקות
                    binding.rvRecentScans.visibility = View.GONE
                }
            }
            .addOnFailureListener {

            }
    }

}