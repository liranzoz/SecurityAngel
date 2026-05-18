package com.example.securityangel.ui.dash

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.R
import com.example.securityangel.data.models.ScanHistoryItem
import com.example.securityangel.data.models.User
import com.example.securityangel.databinding.ActivityDashboardBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.ui.family.FamilySafetyActivity
import com.example.securityangel.ui.scanner.RecentScansAdapter
import com.example.securityangel.ui.vault.PasswordVaultActivity
import com.example.securityangel.utils.DeviceSecuritySyncManager
import com.example.securityangel.utils.GlobalScoreIntegrator
import com.example.securityangel.utils.PermissionMonitor
import com.example.securityangel.utils.VaultCryptoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var myPersonalScore = 100
    private var cachedUser: User? = null
    private var isReceiverRegistered = false

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            applyPermissionsAndAnimate(myPersonalScore)
        }
    }

    private fun registerPackageReceiver() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            packageChangeReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        isReceiverRegistered = true
    }

    private fun unregisterPackageReceiver() {
        if (!isReceiverRegistered) return
        unregisterReceiver(packageChangeReceiver)
        isReceiverRegistered = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = true)

        binding.rvRecentScans.layoutManager = LinearLayoutManager(this)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("user_$userId")
            lifecycleScope.launch { DeviceSecuritySyncManager.sync(this@DashboardActivity) }
            ensureVaultSaltExists(userId)
        }
        loadRecentScans()
        listenToSecurityStatus()
        buttonHandler()
    }

    override fun onResume() {
        super.onResume()
        registerPackageReceiver()

        cachedUser?.let { calculateDeepScore(it) }
    }

    override fun onPause() {
        super.onPause()
        unregisterPackageReceiver()
    }

    private fun listenToSecurityStatus() {
        if (userId == null) return

        db.collection("users").document(userId).addSnapshotListener { document, _ ->
            if (document == null || !document.exists()) return@addSnapshotListener

            val user = document.toObject(User::class.java) ?: return@addSnapshotListener
            cachedUser = user
            calculateDeepScore(user)
            updateFamilyStatusUI(user.activeRisks.size)
        }
    }

    private fun calculateDeepScore(currentUser: User) {
        db.collection("users").document(userId!!).collection("vault").get()
            .addOnSuccessListener { vaultDocs ->
                val leakedCount = vaultDocs.documents.count { it.getBoolean("isLeaked") == true }
                val newScore = GlobalScoreIntegrator.calculatePersonalScore(leakedCount)

                if (newScore != myPersonalScore) {
                    myPersonalScore = newScore
                    saveScoreToFirebase(newScore)
                } else {
                    myPersonalScore = newScore
                }

                if (currentUser.familyId != null) {
                    checkIfAdminAndCalculate(currentUser.familyId, currentUser.id)
                } else {
                    applyPermissionsAndAnimate(myPersonalScore)
                }
            }
            .addOnFailureListener {

                animateScore(myPersonalScore)
            }
    }

    private fun checkIfAdminAndCalculate(familyId: String, myId: String) {
        db.collection("families").document(familyId).get()
            .addOnSuccessListener { familyDoc ->
                if (familyDoc.getString("adminId") == myId) {
                    calculateFamilyWeightedScore(familyId)
                } else {
                    applyPermissionsAndAnimate(myPersonalScore)
                }
            }
    }

    private fun calculateFamilyWeightedScore(familyId: String) {
        db.collection("users").whereEqualTo("familyId", familyId).get()
            .addOnSuccessListener { membersDocs ->
                val memberScores = membersDocs
                    .filter { it.id != userId }
                    .mapNotNull { it.getLong("securityScore")?.toInt() }

                val familyBlendedScore = GlobalScoreIntegrator.blendFamilyScore(myPersonalScore, memberScores)
                applyPermissionsAndAnimate(familyBlendedScore)
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

    private fun saveScoreToFirebase(score: Int) {
        db.collection("users").document(userId!!)
            .update("securityScore", score)
            .addOnFailureListener {  }
    }

    private fun applyPermissionsAndAnimate(baseScore: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val apps = PermissionMonitor.getInstalledAppsWithPermissions(this@DashboardActivity)
            val integratedResult = GlobalScoreIntegrator.integratePermissionsScore(
                currentGlobalScore = baseScore,
                apps = apps,
                permissionsWeight = 0.3f
            )
            withContext(Dispatchers.Main) {
                animateScore(integratedResult.finalScore)
            }
        }
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

    private fun ensureVaultSaltExists(userId: String) {
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { doc ->
            if (doc.getString("vaultSalt").isNullOrEmpty()) {
                userRef.update("vaultSalt", VaultCryptoManager.generateSalt())
            }
        }
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
