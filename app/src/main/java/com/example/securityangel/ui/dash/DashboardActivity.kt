package com.example.securityangel.ui.dash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.R
import com.example.securityangel.data.models.ScanHistoryItem
import com.example.securityangel.data.models.User
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

    private var myPersonalScore = 100

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

        db.collection("users").document(userId).addSnapshotListener { document, _ ->
            if (document == null || !document.exists()) return@addSnapshotListener

            val user = document.toObject(User::class.java) ?: return@addSnapshotListener
            val activeRisks = user.activeRisks
            calculateDeepScore(user)
            updateFamilyStatusUI(activeRisks.size)
        }
    }

    private fun calculateDeepScore(currentUser: User) {
        val vaultTask = db.collection("users").document(userId!!).collection("vault").get()
        val scansTask = db.collection("users").document(userId).collection("scans")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(vaultTask, scansTask)
            .addOnSuccessListener { results ->
                val vaultDocs = results[0] as com.google.firebase.firestore.QuerySnapshot
                val scanDocs = results[1] as com.google.firebase.firestore.QuerySnapshot

                var score = 100
                val leakedCount = vaultDocs.documents.count { it.getBoolean("isLeaked") == true }
                score -= (leakedCount * 10)

                if (score < 20) score = 20

                myPersonalScore = score
                saveScoreToFirebase(score)

                if (currentUser.familyId != null) {
                    checkIfAdminAndCalculate(currentUser.familyId, currentUser.id)
                } else {
                    animateScore(score)
                }
            }
    }

    private fun checkIfAdminAndCalculate(familyId: String, myId: String) {
        db.collection("families").document(familyId).get()
            .addOnSuccessListener { familyDoc ->
                val adminId = familyDoc.getString("adminId")

                if (adminId == myId) {
                    calculateFamilyWeightedScore(familyId)
                } else {
                    animateScore(myPersonalScore)
                }
            }
    }

    private fun calculateFamilyWeightedScore(familyId: String) {
        db.collection("users").whereEqualTo("familyId", familyId).get()
            .addOnSuccessListener { membersDocs ->
                if (membersDocs.isEmpty) {
                    animateScore(myPersonalScore)
                    return@addOnSuccessListener
                }

                var totalFamilyScore = 0
                var memberCount = 0

                for (doc in membersDocs) {
                    if (doc.id == userId) continue

                    val memberScore = doc.getLong("securityScore")?.toInt() ?: 100
                    totalFamilyScore += memberScore
                    memberCount++
                }

                if (memberCount > 0) {
                    val familyAverage = totalFamilyScore / memberCount
                    val finalWeightedScore = (myPersonalScore * 0.6) + (familyAverage * 0.4)
                    animateScore(finalWeightedScore.toInt())
                } else {
                    animateScore(myPersonalScore)
                }
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

        if (score <= 70) {
            progressBar.progressDrawable.setTint(getColor(R.color.status_warning_text_red))
        }

        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, score)
        animation.duration = 1000
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun saveScoreToFirebase(score: Int) {
        db.collection("users").document(userId!!)
            .update("securityScore", score)
    }

    private fun animateScore(targetScore: Int) {
        binding.tvScoreValue.text = targetScore.toString()
        val progressBar = binding.progressBarScore
        val headerBackground = binding.viewHeaderBackground

        if (targetScore >= 80) {
            headerBackground.background.mutate().setTint((getColor(R.color.primary_green)))
        } else if (targetScore >= 50) {
            headerBackground.background.mutate().setTint((getColor(R.color.status_warning_text)))
        } else {
            headerBackground.background.mutate().setTint((getColor(R.color.status_warning_text_red)))
        }

        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetScore)
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