package com.example.securityangel.ui.base

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securityangel.ui.dash.DashboardActivity
import com.example.securityangel.ui.family.FamilySafetyActivity
import com.example.securityangel.ui.password.PasswordGeneratorActivity
import com.example.securityangel.ui.vault.PasswordVaultActivity
import com.example.securityangel.R
import com.example.securityangel.ui.scanner.SandBoxActivity
import com.example.securityangel.ui.settings.SecurityLogActivity
import com.example.securityangel.ui.settings.SettingsActivity
import com.example.securityangel.data.models.User
import com.example.securityangel.databinding.ActivityBaseBinding
import com.example.securityangel.databinding.NavHeaderBinding
import com.example.securityangel.ui.ai.AIChatActivity
import com.example.securityangel.ui.auth.LoginActivity
import com.example.securityangel.ui.auth.SignUpActivity
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var sharedPrefs : SharedPreferences

    private var privacyOverlay: View? = null
    protected var requireBiometricCheck = true

    companion object {
        private var activeActivities = 0
        private var isAppLocked = true
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        if (this is LoginActivity || this is SignUpActivity) {
            requireBiometricCheck = false
        }
        sharedPrefs  = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        super.setContentView(baseBinding.root)


        ViewCompat.setOnApplyWindowInsetsListener(baseBinding.contentFrame) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = ime.bottom > 0
            val bottomPadding = if (isImeVisible) ime.bottom else systemBars.bottom
            v.setPadding(0, 0, 0, bottomPadding)
            insets
        }

        setSupportActionBar(baseBinding.toolbarBase)
        supportActionBar?.title = ""



        toggle = ActionBarDrawerToggle(
            this,
            baseBinding.drawerLayout,
            baseBinding.toolbarBase,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        baseBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (baseBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })


        handleScreenshot()
        loadUserData()
        handleDrawer()
        handleDarkMode()
        highlightCurrentNavigationItem()

    }

    override fun onStart() {
        super.onStart()
        activeActivities++;
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            activeActivities--
        }
        if (activeActivities == 0) {
            isAppLocked = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireBiometricCheck) {
            checkSecurity()
        }
    }

    private fun checkSecurity() {
        val auth = FirebaseAuth.getInstance()
        val sharedPrefs = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
        val isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)

        if (isAppLocked && auth.currentUser != null && isBiometricEnabled && BiometricManager.isBiometricAvailable(this)) {

            showPrivacyScreen()

            BiometricManager.showBiometricPrompt(
                activity = this,
                onSuccess = {
                    isAppLocked = false
                    hidePrivacyScreen()
                },
                onFailure = {
                    performLogoutAndExit()
                }
            )
        }
    }


    private fun updateMenuBadges(user: User) {
        val navView = baseBinding.navigationViewBase
        val menu = navView.menu

        val hasPasswordRisk = user.activeRisks.contains("risk_password_leak") || user.activeRisks.contains("RISK_PASSWORD_LEAK")
        val vaultItem = menu.findItem(R.id.nav_pass_vault)

        if (hasPasswordRisk) {
            vaultItem.actionView = createBadge("!")
        } else {
            vaultItem.actionView = null
        }

        val familyItem = menu.findItem(R.id.nav_family)
        if (user.riskCount > 0) {
            familyItem.actionView = createBadge(user.riskCount.toString())
        } else {
            familyItem.actionView = null
        }
    }


    private fun performLogoutAndExit() {
        // ניתוק המשתמש מפיירבייס (כמו שביקשת)
        FirebaseAuth.getInstance().signOut()

        toast("Authentication failed. Logged out.")

        // מעבר למסך לוגין וניקוי ההיסטוריה
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // פונקציה לטעינת המסך מה-XML והצגתו
    private fun showPrivacyScreen() {
        if (privacyOverlay == null) {
            val rootView = window.decorView as ViewGroup

            // כאן אנחנו טוענים את ה-XML שיצרנו בשלב 1
            privacyOverlay = layoutInflater.inflate(R.layout.view_biometric_lock, rootView, false)

            rootView.addView(privacyOverlay)
        }
        privacyOverlay?.visibility = View.VISIBLE
    }

    private fun hidePrivacyScreen() {
        privacyOverlay?.visibility = View.GONE
        // אופציונלי: להסיר את ה-View לגמרי כדי לחסוך זיכרון
        // (window.decorView as ViewGroup).removeView(privacyOverlay)
        // privacyOverlay = null
    }

    fun createBadge(text: String): View {
        val view = android.view.LayoutInflater.from(this).inflate(R.layout.layout_menu_badge, null)
        val tv = view.findViewById<android.widget.TextView>(R.id.tvBadge)
        tv.text = text
        return view
    }
    private fun highlightCurrentNavigationItem(){
        val navView = baseBinding.navigationViewBase

        val targetId = when (this) {
            is DashboardActivity -> R.id.nav_dash
            is FamilySafetyActivity -> R.id.nav_family
            is SandBoxActivity -> R.id.nav_sand_box
            is PasswordVaultActivity -> R.id.nav_pass_vault
            is SettingsActivity -> R.id.nav_settings
            is PasswordGeneratorActivity -> R.id.nav_generator
            is SecurityLogActivity -> R.id.nav_Logs
            is AIChatActivity -> R.id.nav_ai_chat
            else -> -1
        }
        if (targetId != -1) {
            navView.setCheckedItem(targetId)
        }


    }


   private fun handleScreenshot(){
       val preventScreenshots = sharedPrefs.getBoolean("prevent_screenshots", false)
       if (preventScreenshots) {
           window.setFlags(
               WindowManager.LayoutParams.FLAG_SECURE,
               WindowManager.LayoutParams.FLAG_SECURE
           )
       }

   }
    private fun handleDrawer(){
        baseBinding.navigationViewBase.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dash -> {
                    if (this !is DashboardActivity){
                        openFromDrawer(DashboardActivity::class.java)
                    }

                }
                R.id.nav_family -> {
                    if (this !is FamilySafetyActivity){
                        openFromDrawer(FamilySafetyActivity::class.java)
                    }
                }
                R.id.nav_sand_box -> {
                    if (this !is SandBoxActivity){
                        openFromDrawer(SandBoxActivity::class.java)
                    }
                }
                R.id.nav_pass_vault -> {
                    if (this !is PasswordVaultActivity){
                        openFromDrawer(PasswordVaultActivity::class.java)
                    }
                }
                R.id.nav_settings -> {
                    if (this !is SettingsActivity){
                        openFromDrawer(SettingsActivity::class.java)
                    }
                }
                R.id.nav_generator -> {
                    if (this !is PasswordGeneratorActivity){
                        openFromDrawer(PasswordGeneratorActivity::class.java)
                    }
                }
                R.id.nav_Logs -> {
                    if (this !is SecurityLogActivity){
                        openFromDrawer(SecurityLogActivity::class.java)
                    }
                }
                R.id.nav_ai_chat -> {
                    if (this !is AIChatActivity){
                        openFromDrawer(AIChatActivity::class.java)
                    }
                }
            }

            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
    fun handleDarkMode(){
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

    }
    private fun openFromDrawer(target: Class<out Activity>) {
        val intent = Intent(this, target)

        when (target) {
            DashboardActivity::class.java -> {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)

                if (this !is DashboardActivity) finish()
            }

            else -> {
                startActivity(intent)

                if (this !is DashboardActivity) finish()
            }
        }
    }

    fun setContent(view: View) {
        baseBinding.contentFrame.addView(view)
    }

    fun setToolbarIconColor(isDarkBackground: Boolean) {
        val color = if (isDarkBackground) {
            Color.WHITE
        } else {
            Color.BLACK //
        }

        toggle.drawerArrowDrawable.color = color
    }


    protected fun fetchUserDetails(onSuccess: (User) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        onSuccess(user)
                    }
                }
            }
    }

    private fun loadUserData() {
        fetchUserDetails { user ->
            updateNavigationHeader(user)
            updateMenuBadges(user)
        }
    }
    protected fun getAvatarAnimation(gender: String): Int {
        return when (gender) {
            "Male" -> R.raw.anim_male_avater
            "Female" -> R.raw.anim_female_avater
            else -> R.raw.anim_else_avatar
        }
    }
    private fun updateNavigationHeader(user: User) {
        val headerView = baseBinding.navigationViewBase.getHeaderView(0)
        val headerBinding = NavHeaderBinding.bind(headerView)

        headerBinding.tvUserName.text = "${user.firstName} ${user.lastName}"
        headerBinding.tvUserEmail.text = user.email

        val animationResId = getAvatarAnimation(user.gender)

        headerBinding.lottieProfile.setAnimation(animationResId)
        headerBinding.lottieProfile.playAnimation()
    }

    fun setToolbarVisibility(isVisible: Boolean) {
        if (isVisible) {
            baseBinding.toolbarBase.visibility = View.VISIBLE
        } else {
            baseBinding.toolbarBase.visibility = View.GONE
        }
    }

    fun setToolbarElevation(elevation: Float) {
        baseBinding.toolbarBase.elevation = elevation
    }

    fun openDrawer() {
        baseBinding.drawerLayout.openDrawer(GravityCompat.START)
    }
    abstract fun buttonHandler()
}