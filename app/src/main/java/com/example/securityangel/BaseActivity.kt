package com.example.securityangel

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securityangel.databinding.ActivityBaseBinding

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        super.setContentView(baseBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(baseBinding.drawerLayout) { v, insets ->
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
                    if (this !is PasswordVaultActivity){
                        openFromDrawer(SettingsActivity::class.java)
                    }
                }
            }

            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
            true
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

    abstract fun buttonHandler()
}