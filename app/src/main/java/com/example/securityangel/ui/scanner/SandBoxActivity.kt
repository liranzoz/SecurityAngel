package com.example.securityangel.ui.scanner

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.R
import com.example.securityangel.data.models.ScanHistoryItem
import com.example.securityangel.data.models.VtAttributes
import com.example.securityangel.data.models.VtResponse
import com.example.securityangel.databinding.ActivitySandBoxBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.VirusTotalApi
import com.example.securityangel.utils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SandBoxActivity : BaseActivity() {

    private lateinit var binding: ActivitySandBoxBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySandBoxBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        setToolbarVisibility(false)

        buttonHandler()
        binding.rvResults.layoutManager = LinearLayoutManager(this)

    }

    private fun performScan(url: String) {
        binding.btnScan.text = "Scanning..."
        binding.btnScan.isEnabled = false

        val api = VirusTotalApi.create()
        val urlId = VirusTotalApi.urlToBase64(url)

        api.scanUrl(urlId).enqueue(object : Callback<VtResponse> {
            override fun onResponse(call: Call<VtResponse>, response: Response<VtResponse>) {
                binding.btnScan.text = "Scan"
                binding.btnScan.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data
                    updateUI(data?.attributes)
                } else {
                    toast("Scan failed or URL not found in DB")
                }
            }

            override fun onFailure(call: Call<VtResponse>, t: Throwable) {
                binding.btnScan.text = "Scan"
                binding.btnScan.isEnabled = true
                toast("Error: ${t.message}")
            }
        })
    }

    private fun updateUI(attributes: VtAttributes?) {
        if (attributes == null) return

        val maliciousCount = attributes.stats?.malicious ?: 0
        if (maliciousCount > 0) {
            binding.tvSafeTitle.text = "Unsafe!"
            binding.tvSafeSubtitle.text = "Found $maliciousCount threats on this site."
            binding.iconContainer.setBackgroundResource(R.drawable.bg_circle_light_red)
            binding.tvSafeTitle.setTextColor(getColor(R.color.status_unsafe_text))
            binding.icSafeUnsafe.setImageResource(R.drawable.ic_warning)
            binding.icSafeUnsafe.setColorFilter(getColor(R.color.white))

            saveScanToHistory(binding.etUrlInput.text.toString(),isSafe = false)
        }
        else {
            binding.tvSafeTitle.text = "Safe"
            binding.tvSafeTitle.setTextColor(getColor(android.R.color.holo_green_light))
            binding.iconContainer.setBackgroundResource(R.drawable.bg_circle_light_teal)
            binding.tvSafeTitle.setTextColor(getColor(R.color.primary_green))
            binding.icSafeUnsafe.setImageResource(R.drawable.ic_shield_check)
            binding.tvSafeSubtitle.text = "This website appears to be safe."
            saveScanToHistory(binding.etUrlInput.text.toString(),isSafe = true)
        }

        val resultsList = mutableListOf<ScanResult>()

        val vendorsToShow = listOf(
            "Kaspersky", "Google Safebrowsing", "PhishTank", "OpenPhish",
            "BitDefender", "Sophos", "ESET", "Avast", "Avira",
            "Microsoft", "McAfee", "Malwarebytes", "Fortinet",
            "TrendMicro", "GData", "Symantec", "F-Secure",
            "Panda", "Dr.Web", "Forcepoint ThreatSeeker", "Barracuda"
        )

        attributes.results?.forEach { (engineName, resultData) ->
            val cleanName = resultData.engineName ?: engineName
            val status = resultData.result ?: "Unknown"
            val isSuspicious = status != "clean" && status != "unrated"

            if (vendorsToShow.contains(cleanName) || isSuspicious) {
                val status = resultData.result ?: "clean"
                resultsList.add(ScanResult(cleanName, status))
            }
            resultsList.sortByDescending { it.status != "clean" && it.status != "unrated"
            }
            binding.rvResults.adapter = ScanResultsAdapter(resultsList)
        }
    }

    override fun buttonHandler() {
        binding.btnScan.setOnClickListener {
            val urlToScan = binding.etUrlInput.text.toString().trim()
            if (urlToScan.isNotEmpty()) {
                performScan(urlToScan)
            } else {
                toast("Please enter a URL")
            }
        }

        val btnInfo =  binding.btnInfo
        btnInfo.isFocusable = true
        btnInfo.isClickable = true
        btnInfo.setOnClickListener {
            showInfoDialog()
        }

        binding.btnBack.setOnClickListener { openDrawer() }

        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount!! > 0) {
                val clipData = clipboard.primaryClip?.getItemAt(0)
                val pasteText = clipData?.text.toString()
                if (pasteText.isNotEmpty()) {
                    binding.etUrlInput.setText(pasteText)
                    binding.etUrlInput.setSelection(pasteText.length)
                } else {
                    toast("Clipboard is empty")
                }
            } else {
                toast("Nothing to paste")
            }
        }
    }

    private fun showInfoDialog(){
        MaterialAlertDialogBuilder(this)
            .setTitle("How it works")
            .setMessage(
                "Our scanner analyzes URLs using over 70 antivirus engines to detect malware, phishing, and other threats.\n\n" +
                        "• Green check: The site is safe.\n" +
                        "• Red warning: Threats were detected.\n\n" +
                        "Disclaimer:\n" +
                        "Since the system checks extensive sources, results may occasionally be inaccurate. Accessing any website is strictly at your own risk.\n\n" +
                        "Stay safe and never enter passwords on suspicious sites!"
            )
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
            }.setIcon(R.drawable.ic_info)
            .show()

    }

    private fun saveScanToHistory(url: String, isSafe: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val status = if (isSafe) "safe" else "unsafe"
        val scanItem = ScanHistoryItem(
            url = url,
            status = status,
            timestamp = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("scans")
            .add(scanItem)
    }
}