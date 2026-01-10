package com.example.securityangel

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

open class BaseActivity : AppCompatActivity() {

    private lateinit var baseBinding: ActivityBaseBinding
    // שמרנו את ה-toggle כמשתנה כדי שנוכל לגשת אליו ולשנות צבעים אח"כ
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // הופך את הסטטוס בר לשקוף

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        super.setContentView(baseBinding.root)

        // טיפול ב-Padding (כדי שהמגירה לא תסתיר את הסטטוס בר)
        ViewCompat.setOnApplyWindowInsetsListener(baseBinding.drawerLayout) { v, insets ->
            insets
        }

        // הגדרת הטולבר
        setSupportActionBar(baseBinding.toolbarBase)
        supportActionBar?.title = ""

        // הגדרת המגירה וה-Toggle
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

        // ניהול הלחיצות בתפריט הצד
        baseBinding.navigationViewBase.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is DashboardActivity) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                }
                R.id.nav_family -> {
                    if (this !is FamilySafetyActivity) {
                        startActivity(Intent(this, FamilySafetyActivity::class.java))
                        finish() // אופציונלי
                    }
                }
            }
            baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
            true
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
}