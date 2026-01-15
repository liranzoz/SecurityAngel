package com.example.securityangel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.example.securityangel.databinding.ActivityFamilySafetyBinding

class FamilySafetyActivity : BaseActivity() {

    private lateinit var binding: ActivityFamilySafetyBinding

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
        fetchUserDetails { currentUser ->
            val familyId = currentUser.familyId

            if (familyId == null) {
                toast("No family members")
                binding.membersContainer.removeAllViews()
            } else {
                FamilyRepository.getFamilyData(
                    familyId = familyId,
                    onSuccess = { family ->
                        FamilyRepository.getFamilyMembers(familyId) { members ->
                            updateMembersUI(members, family.adminId)
                        }
                    },
                    onFailure = {
                        toast("Failed to load family info")
                    }
                )
            }
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
            divider.layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2
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
}