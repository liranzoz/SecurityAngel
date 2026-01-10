package com.example.securityangel

import android.os.Bundle
import com.example.securityangel.databinding.ActivityDashboardBinding

// שינוי 1: ירושה מ-BaseActivity במקום AppCompatActivity
class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContent(binding.root)

        // אומרים למגירה: יש כאן רקע ירוק/כהה, אז שים אייקון לבן
        setToolbarIconColor(isDarkBackground = true)
    }
}