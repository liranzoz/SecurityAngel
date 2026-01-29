package com.example.securityangel.ui.family

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.securityangel.data.repo.SecurityLogger
import com.example.securityangel.databinding.ActivityJoinFamilyBinding
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JoinFamilyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinFamilyBinding
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinFamilyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnJoin.setOnClickListener {
            val code = binding.etCode.text.toString().trim()

            if (code.length != 6) {
                binding.tilCode.error = "Please enter a valid 6-digit code"
                return@setOnClickListener
            }

            verifyInvitation(code)
        }

        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() && (clipboard.primaryClip?.itemCount ?: 0) > 0) {
                val pasteText = clipboard.primaryClip?.getItemAt(0)?.text.toString()
                val numericCode = pasteText.filter { it.isDigit() }

                if (numericCode.isNotEmpty()) {
                    binding.etCode.setText(numericCode)
                    binding.etCode.setSelection(numericCode.length)
                    binding.tilCode.error = null
                } else {
                    toast("Clipboard does not contain a code")
                }
            }
        }
    }

    private fun verifyInvitation(inputCode: String) {
        if (currentUser == null || currentUser.email == null) {
            toast("Error: User not identified")
            return
        }

        setLoading(true)
        val userEmail = currentUser.email!!

        db.collection("invitations").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val serverCode = document.getString("code")
                    val familyId = document.getString("familyId")

                    if (serverCode == inputCode && familyId != null) {
                        joinFamilySuccess(familyId, userEmail)
                    } else {
                        setLoading(false)
                        binding.tilCode.error = "Invalid code. Please try again."
                    }
                } else {
                    setLoading(false)
                    binding.tilCode.error = "No invitation found for $userEmail"
                }
            }
            .addOnFailureListener {
                setLoading(false)
                toast("Connection error: ${it.message}")
            }
    }

    private fun joinFamilySuccess(familyId: String, emailDocId: String) {
        val userId = currentUser!!.uid

        db.collection("users").document(userId)
            .update("familyId", familyId)
            .addOnSuccessListener {

                db.collection("invitations").document(emailDocId).delete()
                SecurityLogger.logEvent(
                    SecurityLogger.TYPE_FAMILY_UPDATE,
                    "Family update",
                    "Member with email: $emailDocId joined the family!"
                )
                toast("Welcome to the family! 🎉")

                val intent = Intent(this, FamilySafetyActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                SecurityLogger.logEvent(
                    SecurityLogger.TYPE_FAMILY_UPDATE,
                    "Family update",
                    "Member with email: $emailDocId tried to join and failed"
                )
                toast("Failed to join family: ${it.message}")
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnJoin.isEnabled = !isLoading
        binding.etCode.isEnabled = !isLoading
    }
}