package com.example.securityangel.ui.autofill

import android.content.Intent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.text.InputType
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.EditText
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.securityangel.R
import com.example.securityangel.utils.BiometricKeyStoreHelper
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.VaultCryptoManager
import com.example.securityangel.utils.VaultSessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutofillUnlockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DOMAIN      = "autofill_domain"
        const val EXTRA_USERNAME_ID = "autofill_username_id"
        const val EXTRA_PASSWORD_ID = "autofill_password_id"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val userId get() = FirebaseAuth.getInstance().currentUser?.uid

    private var hasPrompted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (VaultSessionManager.isValid) {

            fillAndReturn(VaultSessionManager.masterPin, VaultSessionManager.vaultSalt)
        }

    }

    override fun onResume() {
        super.onResume()
        if (!hasPrompted && !VaultSessionManager.isValid) {
            hasPrompted = true
            fetchSaltThenUnlock()
        }
    }

    private fun fetchSaltThenUnlock() {
        val uid = userId ?: run { finish(); return }
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val salt = doc.getString("vaultSalt")
                if (salt.isNullOrEmpty()) { finish(); return@addOnSuccessListener }
                attemptUnlock(salt)
            }
            .addOnFailureListener { finish() }
    }

    private fun attemptUnlock(vaultSalt: String) {
        val biometricEnabled = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("biometric_enabled", false)

        if (biometricEnabled
            && BiometricManager.isBiometricAvailable(this)
            && BiometricKeyStoreHelper.hasPinStored(this)
        ) {
            BiometricKeyStoreHelper.showDecryptPrompt(
                activity = this,
                title    = "Unlock Security Angel",
                subtitle = "Authenticate to fill your credentials",
                onSuccess = { pin -> onPinObtained(pin, vaultSalt) },
                onFailure  = { showPinDialog(vaultSalt) }
            )
        } else {
            showPinDialog(vaultSalt)
        }
    }

    private fun showPinDialog(vaultSalt: String) {
        val pinInput = EditText(this).apply {
            hint      = "Enter your Vault PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(64, 32, 64, 16)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Unlock Vault")
            .setMessage("Enter your PIN to fill saved credentials.")
            .setView(pinInput)
            .setCancelable(false)
            .setPositiveButton("Unlock") { _, _ ->
                val pin = pinInput.text.toString()
                if (pin.isNotEmpty()) onPinObtained(pin, vaultSalt) else finish()
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun onPinObtained(pin: String, vaultSalt: String) {
        VaultSessionManager.save(pin, vaultSalt)
        fillAndReturn(pin, vaultSalt)
    }

    private fun fillAndReturn(masterPin: String, vaultSalt: String) {
        val domain      = intent.getStringExtra(EXTRA_DOMAIN) ?: ""
        val usernameId  = intentParcelable<AutofillId>(EXTRA_USERNAME_ID)
        val passwordId  = intentParcelable<AutofillId>(EXTRA_PASSWORD_ID)
        val uid         = userId ?: run { finish(); return }

        firestore.collection("users").document(uid).collection("vault").get()
            .addOnSuccessListener { documents ->
                lifecycleScope.launch {
                    val responseBuilder = FillResponse.Builder()

                    val requiredId = passwordId ?: usernameId
                    if (requiredId != null) {
                        val sib = SaveInfo.Builder(
                            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            arrayOf(requiredId)
                        ).setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                        if (requiredId === passwordId && usernameId != null)
                            sib.setOptionalIds(arrayOf(usernameId))
                        responseBuilder.setSaveInfo(sib.build())
                    }

                    var datasetCount = 0
                    for (doc in documents) {
                        val storedDomain = doc.getString("domain") ?: continue
                        if (!domainsMatch(domain, storedDomain)) continue

                        val email    = withContext(Dispatchers.IO) {
                            VaultCryptoManager.decrypt(doc.getString("email")    ?: "", masterPin, vaultSalt)
                        }
                        val password = withContext(Dispatchers.IO) {
                            VaultCryptoManager.decrypt(doc.getString("password") ?: "", masterPin, vaultSalt)
                        }
                        val siteName = doc.getString("siteName") ?: storedDomain
                        if (password.isEmpty()) continue

                        val views = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                            setTextViewText(R.id.tv_autofill_title, siteName)
                            setTextViewText(R.id.tv_autofill_subtitle, email.ifEmpty { siteName })
                        }
                        @Suppress("DEPRECATION")
                        val dsBuilder = Dataset.Builder(views)
                        usernameId?.let { dsBuilder.setValue(it, AutofillValue.forText(email)) }
                        passwordId?.let { dsBuilder.setValue(it, AutofillValue.forText(password)) }
                        responseBuilder.addDataset(dsBuilder.build())
                        datasetCount++
                    }

                    val resultIntent = Intent().apply {
                        putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, responseBuilder.build())
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
            .addOnFailureListener {

                setResult(RESULT_CANCELED)
                finish()
            }
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T : android.os.Parcelable> intentParcelable(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(key, T::class.java)
        else
            intent.getParcelableExtra(key)

    private fun domainsMatch(current: String, stored: String): Boolean {
        if (current.equals(stored, ignoreCase = true)) return true
        return rootDomain(current).equals(rootDomain(stored), ignoreCase = true)
    }

    private fun rootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) "${parts[parts.size - 2]}.${parts[parts.size - 1]}" else domain
    }
}
