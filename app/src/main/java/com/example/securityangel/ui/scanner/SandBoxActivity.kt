package com.example.securityangel.ui.scanner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val api = VirusTotalApi.create()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySandBoxBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        setToolbarVisibility(false)

        buttonHandler()
        binding.rvResults.layoutManager = LinearLayoutManager(this)
    }

    // === שלב 1: בדיקה אם הלינק כבר קיים במאגר ===
    private fun startScanFlow(url: String) {
        setLoadingState("Checking Database...")
        val urlId = VirusTotalApi.urlToBase64(url)

        api.scanUrl(urlId).enqueue(object : Callback<VtResponse> {
            override fun onResponse(call: Call<VtResponse>, response: Response<VtResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // יש תוצאה במאגר! מציגים מיד.
                    showResults(response.body()!!.data?.attributes, url)
                } else if (response.code() == 404) {
                    // הלינק לא קיים - שולחים לסריקה חדשה
                    submitNewScan(url)
                } else {
                    resetButton()
                    toast("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<VtResponse>, t: Throwable) {
                resetButton()
                toast("Connection Error: ${t.message}")
            }
        })
    }

    // === שלב 2: שליחת לינק חדש לסריקה ===
    private fun submitNewScan(url: String) {
        setLoadingState("Submitting URL...")

        api.submitUrlForScanning(url).enqueue(object : Callback<VtResponse> {
            override fun onResponse(call: Call<VtResponse>, response: Response<VtResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // קיבלנו מזהה סריקה (Analysis ID)
                    val analysisId = response.body()!!.data?.id
                    if (analysisId != null) {
                        // מתחילים לעקוב אחרי הסריקה
                        pollAnalysisStatus(analysisId, url)
                    } else {
                        resetButton()
                        toast("Error: No analysis ID returned")
                    }
                } else {
                    resetButton()
                    toast("Submission Failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<VtResponse>, t: Throwable) {
                resetButton()
                toast("Submission Error: ${t.message}")
            }
        })
    }

    // === שלב 3: בדיקת סטטוס (Polling) בלולאה ===
    private fun pollAnalysisStatus(analysisId: String, originalUrl: String) {
        setLoadingState("Analyzing with 70 engines...")

        api.getAnalysisStatus(analysisId).enqueue(object : Callback<VtResponse> {
            override fun onResponse(call: Call<VtResponse>, response: Response<VtResponse>) {
                if (response.isSuccessful) {
                    val status = response.body()?.data?.attributes?.status

                    if (status == "completed") {
                        // === הסריקה הסתיימה! מושכים את התוצאה הסופית ===
                        fetchFinalReport(originalUrl)
                    } else {
                        // עדיין עובד (queued / in-progress) -> בודקים שוב עוד 2 שניות
                        handler.postDelayed({
                            pollAnalysisStatus(analysisId, originalUrl)
                        }, 2000)
                    }
                } else {
                    resetButton()
                    toast("Analysis Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<VtResponse>, t: Throwable) {
                // במקרה של ניתוק רגעי, ננסה שוב במקום לקרוס
                handler.postDelayed({ pollAnalysisStatus(analysisId, originalUrl) }, 3000)
            }
        })
    }

    // === שלב 4: משיכת הדוח הסופי והצגה ===
    private fun fetchFinalReport(url: String) {
        setLoadingState("Finalizing Report...")
        val urlId = VirusTotalApi.urlToBase64(url)

        api.scanUrl(urlId).enqueue(object : Callback<VtResponse> {
            override fun onResponse(call: Call<VtResponse>, response: Response<VtResponse>) {
                if (response.isSuccessful) {
                    showResults(response.body()?.data?.attributes, url)
                } else {
                    resetButton()
                    toast("Failed to get final report")
                }
            }
            override fun onFailure(call: Call<VtResponse>, t: Throwable) {
                resetButton()
                toast("Final Fetch Error: ${t.message}")
            }
        })
    }

    // === UI Logic ===

    private fun setLoadingState(text: String) {
        binding.btnScan.text = text
        binding.btnScan.isEnabled = false
    }

    private fun resetButton() {
        binding.btnScan.text = "Scan"
        binding.btnScan.isEnabled = true
    }

    private fun showResults(attributes: VtAttributes?, url: String) {
        resetButton()
        if (attributes == null) return

        val maliciousCount = attributes.stats?.malicious ?: 0
        val isSafe = maliciousCount == 0

        // עדכון כותרת
        if (!isSafe) {
            binding.tvSafeTitle.text = "Unsafe!"
            binding.tvSafeSubtitle.text = "Found $maliciousCount threats."
            binding.iconContainer.setBackgroundResource(R.drawable.bg_circle_light_red)
            binding.tvSafeTitle.setTextColor(getColor(R.color.status_unsafe_text))
            binding.icSafeUnsafe.setImageResource(R.drawable.ic_warning)
            binding.icSafeUnsafe.setColorFilter(getColor(R.color.white))
        } else {
            binding.tvSafeTitle.text = "Safe"
            binding.tvSafeSubtitle.text = "No threats detected."
            binding.iconContainer.setBackgroundResource(R.drawable.bg_circle_light_teal)
            binding.tvSafeTitle.setTextColor(getColor(R.color.primary_green))
            binding.icSafeUnsafe.setImageResource(R.drawable.ic_shield_check)
            binding.icSafeUnsafe.clearColorFilter()
        }

        // שמירה להיסטוריה
        saveScanToHistory(url, isSafe)

        // הכנת הרשימה למטה
        val resultsList = mutableListOf<ScanResult>()
        val vendorsToShow = listOf("Kaspersky", "Google Safebrowsing", "PhishTank", "OpenPhish", "BitDefender", "ESET", "Sophos", "Microsoft", "McAfee")

        attributes.results?.forEach { (engine, data) ->
            val status = data.result ?: "Unknown"
            // מציגים אם זה ברשימה שלנו או אם זה זיהה וירוס
            if (vendorsToShow.contains(data.engineName ?: engine) || (status != "clean" && status != "unrated")) {
                resultsList.add(ScanResult(data.engineName ?: engine, status))
            }
        }

        resultsList.sortByDescending { it.status != "clean" && it.status != "unrated" }
        binding.rvResults.adapter = ScanResultsAdapter(resultsList)
    }

    override fun buttonHandler() {
        binding.btnScan.setOnClickListener {
            val url = binding.etUrlInput.text.toString().trim()
            if (url.isNotEmpty()) startScanFlow(url) else toast("Enter a URL")
        }

        // ... שאר הכפתורים (Paste, Back, Info) ללא שינוי ...
        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                binding.etUrlInput.setText(clip.getItemAt(0).text.toString())
            }
        }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun saveScanToHistory(url: String, isSafe: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val item = ScanHistoryItem(url, if(isSafe) "safe" else "unsafe", System.currentTimeMillis())
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("scans").add(item)
    }
}