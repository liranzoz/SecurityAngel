package com.example.securityangel.ui.family

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.securityangel.data.models.User
import com.example.securityangel.data.repo.FamilyRepository
import com.example.securityangel.data.repo.SecurityLogger
import com.example.securityangel.databinding.ActivityAddMemberBinding
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddMemberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMemberBinding
    private var currentFamilyId: String? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialData()
        setupButtons()
    }

    private fun setupInitialData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                currentUser = document.toObject(User::class.java)
                currentFamilyId = currentUser?.familyId

                if (currentFamilyId == null) {
                    binding.btnAddBtn.text = "Create Family & Add Member"
                }
            }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAddBtn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            if (currentUser == null) return@setOnClickListener

            setLoading(true)

            if (currentFamilyId != null) {
                generateCodeAndInvite(email)
            } else {
                createNewFamilyAndAddMember(email)
            }

        }
    }

    private fun createNewFamilyAndAddMember(emailToAdd: String) {
        val admin = currentUser!!
        val familyName = "${admin.lastName} Family"

        FamilyRepository.createFamily(
            admin = admin,
            familyName = familyName,
            onSuccess = { newFamilyId ->
                currentFamilyId = newFamilyId
                generateCodeAndInvite(emailToAdd)
                addMemberToExistingFamily(newFamilyId, emailToAdd)

            },
            onFailure = { error ->
                setLoading(false)
                toast("Failed to create family: $error")
            }
        )
    }

    private fun addMemberToExistingFamily(familyId: String, email: String) {
        FamilyRepository.addMemberByEmail(
            familyId = familyId,
            email = email,
            onSuccess = {

                setLoading(false)
                toast("Member added successfully!")
                finish()
            },
            onFailure = { errorMessage ->
                setLoading(false)

                binding.tilEmail.error = errorMessage
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAddBtn.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }

    private fun generateCodeAndInvite(email: String) {
        val invitationCode = (100000..999999).random().toString()
        val familyId = currentFamilyId ?: return

        val invitation = hashMapOf(
            "email" to email,
            "familyId" to familyId,
            "code" to invitationCode,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        setLoading(true)

        FirebaseFirestore.getInstance().collection("invitations")
            .document(email)
            .set(invitation)
            .addOnSuccessListener {
                setLoading(false)
                shareInvitationCode(email, invitationCode)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Failed to create invitation: ${e.message}")
            }
    }

    private fun shareInvitationCode(email: String, code: String) {
        val shareText = """
            Hey! I want to add you to my family in Security Angel.
            
            1. Download the app.
            2. Register with email: $email
            3. Use this secure code to join: *$code*
            
            Stay safe!
        """.trimIndent()

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Join my Family on Security Angel")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Send Invitation via...")
        startActivity(shareIntent)

        finish()
    }

}