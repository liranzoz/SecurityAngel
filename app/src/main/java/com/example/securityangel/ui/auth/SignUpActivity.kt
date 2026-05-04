package com.example.securityangel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.example.securityangel.ui.dash.DashboardActivity
import com.example.securityangel.R
import com.example.securityangel.data.models.User
import com.example.securityangel.databinding.ActivitySignUpBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : BaseActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private var isGoogleSignIn = false
    private var pendingUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateButtonState(false)
        auth = FirebaseAuth.getInstance()
        buttonHandler()
        isGoogleSignIn = intent.getBooleanExtra("IS_GOOGLE_SIGN_IN", false)

        if (isGoogleSignIn) { setupGoogleMode() }
        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etFirstName.addTextChangedListener(watcher)
        binding.etLastName.addTextChangedListener(watcher)
        binding.etPhone.addTextChangedListener(watcher)
        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
    }
    private fun checkInputs() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        val basicInfoFilled = firstName.isNotEmpty() &&
                lastName.isNotEmpty() &&
                phone.isNotEmpty() &&
                email.isNotEmpty()

        val passwordValid = if (isGoogleSignIn) {
            true
        } else {
            password.length >= 6
        }

        updateButtonState(basicInfoFilled && passwordValid)
    }

    private fun updateButtonState(isEnabled: Boolean) {
        binding.btnRegister.isEnabled = isEnabled
        if (isEnabled) {
            binding.btnRegister.alpha = 1.0f
        } else {
            binding.btnRegister.alpha = 0.5f
        }
    }

    private fun performCancelRegistration() {
        if (isGoogleSignIn) {
            auth.signOut()
            toast("Registration cancelled")
        }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setupGoogleMode() {
        val email = intent.getStringExtra("GOOGLE_EMAIL")
        val name = intent.getStringExtra("GOOGLE_NAME")

        binding.etEmail.setText(email)
        binding.etEmail.isEnabled = false

        if (name != null) {
            val parts = name.split(" ")
            if (parts.isNotEmpty()) binding.etFirstName.setText(parts[0])
            if (parts.size > 1) binding.etLastName.setText(parts.drop(1).joinToString(" "))
        }
        binding.etPassword.visibility = View.GONE
        binding.tvPassword.visibility = View.GONE
    }
    private fun performSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        val gender = when (binding.rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Not Selected"
        }

        if (firstName.isEmpty()) {
            binding.etFirstName.error = "Name required"
            return
        }

        val tempUser = User("", firstName, lastName, email, phone, gender)
        if (isGoogleSignIn) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val finalEmail = email.ifEmpty { currentUser.email ?: "" }
                saveUserToDatabase(currentUser.uid, firstName, lastName, finalEmail, phone, gender)
            } else {
                toast("Error: Session expired, please login again")
                finish()
            }
        } else {
            val password = binding.etPassword.text.toString().trim()
            if (password.length < 6) return

            binding.btnRegister.text = "Creating Account..."
            binding.btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { res ->
                    val user = res.user ?: return@addOnSuccessListener

                    tempUser.id = user.uid
                    pendingUser = tempUser

                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            showVerificationUI(email)
                        }
                        .addOnFailureListener { e ->
                            toast("Failed to send email: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    binding.btnRegister.text = "Sign Up"
                    binding.btnRegister.isEnabled = true

                    if (e is FirebaseAuthUserCollisionException) {
                        binding.etEmail.error = "Email already registered!"
                        toast("This email is already in use. Please login.")
                    } else {
                        toast("Error: ${e.message}")
                    }
                }
        }
    }
    private fun showVerificationUI(email: String) {
        binding.mainFormContainer.visibility = View.GONE
        binding.verificationContainer.visibility = View.VISIBLE
        binding.tvTitle.text = "Check your inbox"
        binding.tvVerificationText.text = "We sent a link to:\n$email\n\nClick it to verify your account."
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser

        user?.reload()?.addOnCompleteListener { task ->
            if (user.isEmailVerified) {
                toast("Email verified successfully!")
                pendingUser?.let { saveUserToDatabase(it) }
            } else {
                toast("Email not verified yet. Please click the link.")
            }
        }
    }
    private fun saveUserToDatabase(uid: String, first: String, last: String, email: String, phone: String, gender: String) {
        val user = User(uid, first, last, email, phone, gender)

        FirebaseFirestore.getInstance().collection("users").document(uid).set(user)
            .addOnSuccessListener {
                toast("Welcome!")
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener { e -> toast("Save failed: ${e.message}") }
    }

    private fun saveUserToDatabase(user: User) {
        FirebaseFirestore.getInstance().collection("users").document(user.id).set(user)
            .addOnSuccessListener {
                toast("Welcome!")
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener { e -> toast("Save failed: ${e.message}") }
    }

    override fun buttonHandler() {
        binding.btnRegister.setOnClickListener {
            performSignUp()
        }

        binding.tvLoginLink.setOnClickListener {
            performCancelRegistration()
        }

        binding.btnBack.setOnClickListener {
            performCancelRegistration()
        }

        binding.btnCheckVerification.setOnClickListener {
            checkEmailVerification()
        }

        binding.btnResendEmail.setOnClickListener {
            auth.currentUser?.sendEmailVerification()
                ?.addOnSuccessListener { toast("Link resent! Check your inbox.") }
                ?.addOnFailureListener { toast("Wait a moment before retrying.") }
        }
    }
}
