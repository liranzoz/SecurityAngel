package com.example.securityangel.ui.family

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.example.securityangel.R
import com.example.securityangel.data.models.User
import com.example.securityangel.databinding.ActivityFamilySafetyBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FamilySafetyActivity : BaseActivity() {

    private lateinit var binding: ActivityFamilySafetyBinding
    private var familyListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilySafetyBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)

        buttonHandler()
        loadFamilyData()
    }

    override fun onResume() {
        super.onResume()
        loadFamilyData()
    }

    private fun loadFamilyData() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val familyId = userDoc.getString("familyId")

                if (familyId == null) {
                    binding.membersContainer.removeAllViews()
                    return@addOnSuccessListener
                }

                startRealtimeFamilyMonitoring(familyId)
            }
    }

    private fun startRealtimeFamilyMonitoring(familyId: String) {
        val db = FirebaseFirestore.getInstance()

        familyListener = db.collection("users")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    toast("Listen failed: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val members = snapshots.toObjects(User::class.java)

                    fetchFamilyAdminId(familyId) { adminId ->
                        updateMembersUI(members, adminId)
                    }
                }
            }
    }

    private fun fetchFamilyAdminId(familyId: String, onFound: (String) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("families").document(familyId).get()
            .addOnSuccessListener { doc ->
                val adminId = doc.getString("adminId") ?: ""
                onFound(adminId)
            }
    }



    private fun updateMembersUI(members: List<User>, adminId: String) {
        binding.membersContainer.removeAllViews()

        for (member in members) {
            val memberView = LayoutInflater.from(this)
                .inflate(R.layout.item_family_member, binding.membersContainer, false)

            val tvName = memberView.findViewById<TextView>(R.id.tvMemberName)
            val tvStatus = memberView.findViewById<TextView>(R.id.tvMemberStatus)
            val lottieAvatar =
                memberView.findViewById<LottieAnimationView>(R.id.lottieMemberAvatar)

            if (member.id == adminId) {
                tvName.text = "${member.firstName} ${member.lastName} (Admin)"
            } else {
                tvName.text = "${member.firstName} ${member.lastName}"
            }

            lottieAvatar.setAnimation(getAvatarAnimation(member.gender))
            lottieAvatar.playAnimation()

            if (member.riskCount > 0) {
                tvStatus.text = "${member.riskCount} Breaches"
                tvStatus.setTextColor(getColor(R.color.status_unsafe_text))
                tvStatus.background?.setTint(getColor(R.color.status_unsafe_bg))
            } else {
                tvStatus.text = "Safe"
                tvStatus.setTextColor(getColor(R.color.status_safe_text))
                tvStatus.background?.setTint(getColor(R.color.status_safe_bg))
            }

            binding.membersContainer.addView(memberView)

            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2
            )
            divider.setBackgroundColor(getColor(R.color.divider))
            binding.membersContainer.addView(divider)
        }
    }

    override fun buttonHandler() {
        binding.btnAddMember.setOnClickListener {
            val intent = Intent(this, AddMemberActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        familyListener?.remove()
    }
}