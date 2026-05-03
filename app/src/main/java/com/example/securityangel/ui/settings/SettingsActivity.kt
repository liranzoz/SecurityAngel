package com.example.securityangel.ui.settings

import android.R
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.autofill.AutofillManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.example.securityangel.data.models.User
import com.example.securityangel.databinding.ActivitySettingsBinding
import com.example.securityangel.ui.auth.LoginActivity
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.ui.family.FamilyManagementActivity
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.VaultSessionManager
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContent(binding.root)
        buttonHandler()
        setToolbarIconColor(isDarkBackground = false)

        sharedPrefs = getSharedPreferences("AppScanSettings", MODE_PRIVATE)

        fetchUserDetails { user ->
            updateUI(user)
        }

        setupBiometricRow()
        setupScreenshotRow()
        loadSettingsUserData()
        setupDarkModeRow()
        setupFamilyManagementRow()
        setupAutofillRow()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the autofill status each time the user returns (e.g. from system settings).
        refreshAutofillRowStatus()
    }

    private fun setupFamilyManagementRow(){

        with(binding.rowFamilyManagement) {
            tvTitle.text = "My Family" // הכותרת

            imgIcon.setImageResource(R.drawable.ic_menu_myplaces)
            imgIcon.setColorFilter(getColor(com.example.securityangel.R.color.primary_green_dark))

            root.setOnClickListener {
                val intent = Intent(this@SettingsActivity, FamilyManagementActivity::class.java)
                startActivity(intent)
            }
        }


    }
    private fun setupDarkModeRow() {
        with(binding.rowDarkMode) {
            tvTitle.text = "Dark Mode"
            imgIcon.setImageResource(R.drawable.ic_menu_day)
            imgArrow.visibility = View.GONE
            switchSetting.visibility = View.VISIBLE

            // Silence the listener before restoring state so that programmatically
            // setting isChecked during onCreate doesn't trigger recreate().
            switchSetting.setOnCheckedChangeListener(null)
            switchSetting.isChecked = sharedPrefs.getBoolean("dark_mode", false)

            switchSetting.setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                // setDefaultNightMode() triggers recreation internally via AppCompat.
                // An explicit recreate() here would cause a second recreation race that
                // manifests as a black flash followed by a revert to the previous theme.
            }
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

    private fun setupBiometricRow() {
        with(binding.rowBiometric) {
            tvTitle.text = "Biometric Unlock"
            imgIcon.setImageResource(R.drawable.ic_lock_idle_lock)
            imgIcon.setColorFilter(getColor(com.example.securityangel.R.color.primary_green_dark))

            imgArrow.visibility = View.GONE
            switchSetting.visibility = View.VISIBLE

            val isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)
            switchSetting.isChecked = isBiometricEnabled

            switchSetting.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (!BiometricManager.isBiometricAvailable(this@SettingsActivity)) {
                        toast("Biometric authentication is not available on this device")
                        buttonView.isChecked = false
                        return@setOnCheckedChangeListener
                    }
                }
                sharedPrefs.edit().putBoolean("biometric_enabled", isChecked).apply()
            }
        }
    }

    private fun setupScreenshotRow() {
        with(binding.rowScreenshot) {
            tvTitle.text = "Prevent Screenshots"
            imgIcon.setImageResource(R.drawable.ic_menu_camera)
            imgIcon.setColorFilter(getColor(com.example.securityangel.R.color.primary_green_dark))

            imgArrow.visibility = View.GONE
            switchSetting.visibility = View.VISIBLE

            val isScreenshotPrevented = sharedPrefs.getBoolean("prevent_screenshots", false)
            switchSetting.isChecked = isScreenshotPrevented

            switchSetting.setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean("prevent_screenshots", isChecked).apply()

                setScreenProtection(isChecked)

                toast(if (isChecked) "Screenshots blocked" else "Screenshots allowed")
            }
        }
    }

    private fun setScreenProtection(prevent: Boolean) {
        if (prevent) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun setupAutofillRow() {
        with(binding.rowAutofill) {
            imgIcon.setImageResource(com.example.securityangel.R.drawable.ic_password)
            imgIcon.setColorFilter(getColor(com.example.securityangel.R.color.primary_green_dark))
            imgArrow.visibility    = View.VISIBLE
            switchSetting.visibility = View.GONE

            root.setOnClickListener {
                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
        refreshAutofillRowStatus()
    }

    private fun refreshAutofillRowStatus() {
        val afm = getSystemService(AutofillManager::class.java) ?: return
        val isActive = afm.hasEnabledAutofillServices()
        binding.rowAutofill.tvTitle.text = if (isActive) "Autofill: Active ✓" else "Enable Autofill Service"
    }

    override fun buttonHandler() {
        binding.btnSignOut.setOnClickListener {
            VaultSessionManager.clear()
            FirebaseAuth.getInstance().signOut()
            toast("Signed out successfully")
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}