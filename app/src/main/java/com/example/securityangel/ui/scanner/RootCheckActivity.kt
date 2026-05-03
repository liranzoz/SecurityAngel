package com.example.securityangel.ui.scanner

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.example.securityangel.R
import com.example.securityangel.databinding.ActivityRootCheckBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.RootDetector

class RootCheckActivity : BaseActivity() {

    private lateinit var binding: ActivityRootCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRootCheckBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        setToolbarVisibility(false)
        buttonHandler()
    }

    override fun buttonHandler() {
        binding.btnBack.setOnClickListener { openDrawer() }
        binding.btnScan.setOnClickListener { runScan() }
    }

    private fun runScan() {
        val result = RootDetector.getDetailedResult(this)

        binding.tvSuBinariesResult.text = if (result.suBinariesFound) "Found" else "Clean"
        binding.tvBuildTagsResult.text = if (result.testKeysFound) "Test Keys" else "Clean"
        binding.tvRootAppsResult.text = if (result.rootPackagesFound) "Found" else "Clean"

        applyPillStyle(binding.tvSuBinariesResult, result.suBinariesFound)
        applyPillStyle(binding.tvBuildTagsResult, result.testKeysFound)
        applyPillStyle(binding.tvRootAppsResult, result.rootPackagesFound)

        if (result.isRooted) {
            binding.tvStatusTitle.text = "Device is Rooted"
            binding.tvStatusSubtitle.text = "Root access was detected on this device"
            binding.ivStatusIcon.setImageResource(R.drawable.ic_warning)
            binding.ivStatusIcon.imageTintList =
                ContextCompat.getColorStateList(this, R.color.status_unsafe_text)
            binding.cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.status_unsafe_bg)
            )
        } else {
            binding.tvStatusTitle.text = "Device Not Rooted"
            binding.tvStatusSubtitle.text = "No root access detected"
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
            binding.ivStatusIcon.imageTintList =
                ContextCompat.getColorStateList(this, R.color.primary_green)
            binding.cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.white)
            )
        }
    }

    private fun applyPillStyle(view: android.widget.TextView, isDanger: Boolean) {
        val textColor = if (isDanger) R.color.status_unsafe_text else R.color.status_safe_text
        view.setTextColor(ContextCompat.getColor(this, textColor))
    }
}