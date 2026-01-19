package com.example.securityangel.ui.base

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var sharedPrefs : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

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