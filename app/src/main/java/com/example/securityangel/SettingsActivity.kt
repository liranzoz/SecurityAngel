package com.example.securityangel

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securityangel.databinding.ActivityFamilySafetyBinding
import com.example.securityangel.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContent(binding.root)
        buttonHandler()
        setToolbarIconColor(isDarkBackground = false)
        fetchUserDetails { user ->
            updateUI(user)
        }
    }

    private fun loadSettingsUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        updateUI(user)
                    }
                }
            }
    }
    private fun updateUI(user: User) {
        binding.tvUserName.text = "${user.firstName} ${user.lastName}"
        binding.tvUserEmail.text = user.email

        val animationResId = getAvatarAnimation(user.gender)

        binding.imgProfileContainer.setAnimation(animationResId)
        binding.imgProfileContainer.playAnimation()
    }


    override fun buttonHandler() {
        binding.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            toast( "Signed out successfully")
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}