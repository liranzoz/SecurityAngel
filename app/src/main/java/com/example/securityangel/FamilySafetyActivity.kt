package com.example.securityangel

import android.os.Bundle
import com.example.securityangel.databinding.ActivityFamilySafetyBinding

// ירושה מ-BaseActivity
class FamilySafetyActivity : BaseActivity() {

    private lateinit var binding: ActivityFamilySafetyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilySafetyBinding.inflate(layoutInflater)
        setContent(binding.root)
        setupFamilyMembers()

        // אומרים למגירה: יש כאן רקע לבן, אז שים אייקון שחור!
        setToolbarIconColor(isDarkBackground = false)
    }

    private fun setupFamilyMembers() {
        // שרה
        binding.rowMember1.tvName.text = "Sarah Johnson"
        binding.rowMember1.tvStatus.text = "Safe"

        // מייקל (הטיפול המיוחד בצבעים)
        binding.rowMember2.tvName.text = "Michael Johnson"
        binding.rowMember2.tvStatus.text = "1 Breach"

        // שימוש בצבעים מ-colors.xml
        binding.rowMember2.tvStatus.setTextColor(getColor(R.color.status_unsafe_text))
        binding.rowMember2.tvStatus.backgroundTintList = getColorStateList(R.color.status_unsafe_bg)

        // שאר המשפחה
        binding.rowMember3.tvName.text = "Emma Johnson"
        binding.rowMember4.tvName.text = "David Johnson"
    }
}