package com.example.securityangel.ui.family

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.airbnb.lottie.LottieAnimationView
import com.example.securityangel.R
import com.example.securityangel.data.models.User
import com.example.securityangel.data.repo.FamilyRepository
import com.example.securityangel.databinding.ActivityFamilyManagmentBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.toast

class FamilyManagementActivity : BaseActivity() {
    private lateinit var binding: ActivityFamilyManagmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyManagmentBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)

        buttonHandler()
        loadFamilyDetails()
    }

    override fun onResume() {
        super.onResume()
        loadFamilyDetails()
    }

    private fun loadFamilyDetails() {
        fetchUserDetails { currentUser ->
            val familyId = currentUser.familyId

            if (familyId == null) {
                binding.tvFamilyName.text = "No Family Created"
                binding.membersListContainer.removeAllViews()
                binding.btnJoinWithCode.visibility = View.VISIBLE
                binding.btnJoinWithCode.setOnClickListener {
                    startActivity(Intent(this, JoinFamilyActivity::class.java))
                }
            } else {
                FamilyRepository.getFamilyData(
                    familyId = familyId,
                    onSuccess = { family ->
                        binding.tvFamilyName.text = family.name
                        binding.tvMemberCount.text = "${family.members.size} members"

                        FamilyRepository.getFamilyMembers(familyId) { members ->
                            populateMembersList(members, family.adminId, currentUser.id)
                        }
                    },
                    onFailure = {
                        toast("Failed to load family data")
                    }
                )
            }
        }
    }

    private fun populateMembersList(members: List<User>, adminId: String, currentUserId: String) {
        binding.membersListContainer.removeAllViews()

        val iamAdmin = (currentUserId == adminId)

        for (member in members) {
            val memberView = LayoutInflater.from(this)
                .inflate(R.layout.item_family_member, binding.membersListContainer, false)

            val tvName = memberView.findViewById<TextView>(R.id.tvMemberName)
            val tvStatus = memberView.findViewById<TextView>(R.id.tvMemberStatus)
            val lottieAvatar = memberView.findViewById<LottieAnimationView>(R.id.lottieMemberAvatar)
            val btnRemove = memberView.findViewById<ImageView>(R.id.btnRemoveMember)

            var nameText = "${member.firstName} ${member.lastName}"
            if (member.id == currentUserId) nameText += " (You)"
            tvName.text = nameText

            lottieAvatar.setAnimation(getAvatarAnimation(member.gender))
            lottieAvatar.playAnimation()


            if (member.id == adminId) {
                tvStatus.text = "Admin"
                tvStatus.setTextColor(getColor(R.color.primary_green))
                btnRemove.visibility = View.GONE
            } else {
                tvStatus.text = "Member"
                tvStatus.setTextColor(getColor(R.color.text_secondary))

                if (iamAdmin) {
                    btnRemove.visibility = View.VISIBLE
                    btnRemove.setOnClickListener {
                        showDeleteConfirmation(member)
                    }
                } else {
                    btnRemove.visibility = View.GONE
                }
            }

            binding.membersListContainer.addView(memberView)

            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2
            )
            divider.setBackgroundColor(getColor(R.color.divider))
            binding.membersListContainer.addView(divider)
        }
    }

    private fun showDeleteConfirmation(member: User) {
        AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${member.firstName} from the family?")
            .setPositiveButton("Remove") { _, _ ->
                fetchUserDetails { currentUser ->
                    currentUser.familyId?.let { fId ->
                        FamilyRepository.removeMember(
                            familyId = fId,
                            memberId = member.id,
                            onSuccess = {
                                toast("${member.firstName} removed successfully")
                                loadFamilyDetails()
                            },
                            onFailure = { err ->
                                toast(err)
                            }
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun buttonHandler() {
        binding.fabAddMember.setOnClickListener {
            val intent = Intent(this, AddMemberActivity::class.java)
            startActivity(intent)

            binding.btnBack.setOnClickListener { finish() }
        }
    }
}