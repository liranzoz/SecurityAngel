package com.example.securityangel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import com.example.securityangel.ui.dash.DashboardActivity
import com.example.securityangel.databinding.ActivityLoginBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToDashboard()
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {

            val sharedPrefs = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            val isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)

            if (isBiometricEnabled && BiometricManager.isBiometricAvailable(this)) {
                BiometricManager.showBiometricPrompt(
                    activity = this,
                    onSuccess = {
                        navigateToDashboard()
                    },
                    onFailure = {
                        // כישלון או ביטול - נשארים במסך ההתחברות
                        // (אופציונלי: אפשר לנתק את המשתמש אם רוצים להכריח סיסמה)
                    }
                )
            } else {
                navigateToDashboard()
            }
        }

        buttonHandler()

    }

    override fun buttonHandler() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Valid email required"
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password required"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToDashboard()
                } else {
                    toast("Login Failed: ${task.exception?.message}")
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}