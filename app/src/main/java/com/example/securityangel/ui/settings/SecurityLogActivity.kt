package com.example.securityangel.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.data.repo.FamilyRepository
import com.example.securityangel.data.repo.SecurityRepository
import com.example.securityangel.databinding.ActivitySecurityLogBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth

class SecurityLogActivity : BaseActivity() {

    private lateinit var binding: ActivitySecurityLogBinding
    private lateinit var adapter: SecurityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityLogBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        setToolbarVisibility(false)

        setupRecyclerView()
        checkAdminAndLoadLogs()
        buttonHandler()

    }

    override fun buttonHandler() {
        binding.btnClearLogs.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Activity Log")
                .setMessage("Are you sure you want to delete all history?")
                .setPositiveButton("Delete") { _, _ ->
                    fetchUserDetails { user ->
                        user.familyId?.let { familyId ->
                            SecurityRepository.clearFamilyLogs(
                                familyId = familyId,
                                onSuccess = {
                                    toast("Logs cleared successfully")
                                },
                                onFailure = { error ->
                                    toast(error)
                                }
                            )
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnBack.setOnClickListener { openDrawer()}
    }

    private fun setupRecyclerView() {
        adapter = SecurityLogAdapter(emptyList())
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter
    }

    private fun checkAdminAndLoadLogs() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        fetchUserDetails { user ->
            val familyId = user.familyId

            if (familyId == null) {
                toast("You are not part of a family group")
                finish()
                return@fetchUserDetails
            }

            FamilyRepository.getFamilyData(
                familyId = familyId,
                onSuccess = { family ->
                    if (family.adminId == currentUserId) {
                        loadLogs(familyId)
                    } else {
                        toast("Access Denied: Only admin can view activity logs")
                        finish()
                    }
                },
                onFailure = {
                    toast("Error verifying permissions")
                    finish()
                }
            )
        }
    }

    private fun loadLogs(familyId: String) {
        SecurityRepository.getFamilyLogs(familyId) { logs ->
            if (logs.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvLogs.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvLogs.visibility = View.VISIBLE
                adapter.updateList(logs)
            }
        }
    }

}
