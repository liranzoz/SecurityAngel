package com.example.securityangel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.example.securityangel.R
import com.example.securityangel.ui.dash.DashboardActivity
import com.example.securityangel.databinding.ActivityLoginBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
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
                        toast("Authentication failed. Please login manually.")
                    }
                )
            } else {
                navigateToDashboard()
            }
        }

        buttonHandler()

    }
    private fun setupGoogleSignIn() {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        binding.btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val googleEmail = account.email
                firebaseAuthWithGoogle(account.idToken!!, googleEmail)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                toast("Google sign in failed: ${e.statusCode}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, googleEmail: String?) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    checkAndCreateUserInFirestore(user,googleEmail)

                } else {
                    toast("Firebase auth failed")
                }
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
                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {
                        navigateToDashboard()
                    } else {
                        toast("Please verify your email address first.")
                        auth.signOut()
                    }

                } else {
                    toast("Login Failed: ${task.exception?.message}")
                }
            }
    }
    private fun checkAndCreateUserInFirestore(firebaseUser: com.google.firebase.auth.FirebaseUser?,googleEmail: String?) {
        if (firebaseUser == null) return

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(firebaseUser.uid)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                navigateToDashboard()
            } else {
                val emailToSend = googleEmail ?: firebaseUser.email ?: ""
                val intent = Intent(this, SignUpActivity::class.java)

                intent.putExtra("IS_GOOGLE_SIGN_IN", true)
                intent.putExtra("GOOGLE_EMAIL", emailToSend)
                intent.putExtra("GOOGLE_NAME", firebaseUser.displayName)

                startActivity(intent)
                finish()
            }
        }
    }
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun buttonHandler() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}