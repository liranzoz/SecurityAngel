package com.example.securityangel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import com.example.securityangel.ui.dash.DashboardActivity
import com.example.securityangel.R
import com.example.securityangel.data.models.User
import com.example.securityangel.databinding.ActivitySignUpBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : BaseActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        buttonHandler()

    }

    override fun buttonHandler() {
        binding.btnRegister.setOnClickListener {
            performSignUp()
        }

        binding.tvLoginLink.setOnClickListener {
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun performSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        val gender = when (binding.rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Not Selected"
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Valid email required"
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Min 6 chars password"
            return
        }
        if (firstName.isEmpty()) {
            binding.etFirstName.error = "Name required"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                val newUser = User(
                    id = userId,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    gender = gender
                )

                saveUserToDatabase(newUser)
            }
            .addOnFailureListener { e ->
                toast("Sign Up Failed: ${e.message}")
            }
    }

    private fun saveUserToDatabase(user: User) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.id).set(user)
            .addOnSuccessListener {
                toast("Account created & saved!")

                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

            }
            .addOnFailureListener { e ->
                toast("Failed to save user details: ${e.message}")
            }
    }
}