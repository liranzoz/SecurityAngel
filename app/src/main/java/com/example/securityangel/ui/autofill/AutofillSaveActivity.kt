package com.example.securityangel.ui.autofill

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.lifecycleScope
import com.example.securityangel.databinding.ActivityAutofillSaveBinding
import com.example.securityangel.utils.BiometricKeyStoreHelper
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.VaultCryptoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transient overlay activity launched by SecurityAngelAutofillService when the
 * Android autofill framework detects new login credentials.  Extends
 * AppCompatActivity directly (not BaseActivity) to avoid the biometric app-lock
 * cycle and the navigation drawer.
 */
class AutofillSaveActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USERNAME                 = "extra_username"
        const val EXTRA_PASSWORD                 = "extra_password"
        const val EXTRA_DOMAIN                   = "extra_domain"
        const val EXTRA_FROM_CREDENTIAL_PROVIDER = "from_credential_provider"
    }

    private lateinit var binding: ActivityAutofillSaveBinding

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val userId    get() = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var domain: String

    // True when launched by SecurityAngelCredentialProviderService so we know
    // to signal the result back through PendingIntentHandler on save success.
    private var isFromCredentialProvider = false

    // Guard so the biometric prompt fires exactly once across configuration changes.
    private var hasPromptedBiometrics = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutofillSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFromCredentialProvider = intent.getBooleanExtra(EXTRA_FROM_CREDENTIAL_PROVIDER, false)

        if (isFromCredentialProvider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Credential Provider path — the OS injects the full CreatePasswordRequest
            // into the intent via PendingIntentHandler at launch time.
            if (!extractFromCredentialProviderIntent()) return
        } else {
            // Legacy Autofill Service path — credentials arrive as intent extras.
            username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
            password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
            domain   = intent.getStringExtra(EXTRA_DOMAIN)   ?: ""
        }

        if (password.isEmpty() || userId == null) {
            finish()
            return
        }

        populateHeader()
        wireButtons()
        // Biometric prompt is deferred to onResume — calling it here crashes
        // silently because the Window is not yet fully attached to the manager.
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun extractFromCredentialProviderIntent(): Boolean {
        val providerRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        val callingRequest  = providerRequest?.callingRequest

        if (providerRequest == null || callingRequest !is CreatePasswordRequest) {
            finish()
            return false
        }

        username = callingRequest.id
        password = callingRequest.password
        domain   = providerRequest.callingAppInfo.packageName ?: ""
        return true
    }

    override fun onResume() {
        super.onResume()
        if (!hasPromptedBiometrics) {
            hasPromptedBiometrics = true
            attemptBiometricSave()
        }
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun populateHeader() {
        binding.tvDomain.text   = domain.ifEmpty { "Unknown app" }
        binding.tvUsername.text = username.ifEmpty { "Unknown username" }
    }

    private fun wireButtons() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val pin = binding.etMasterPin.text.toString()
            if (pin.isEmpty()) {
                toast("Please enter your Master PIN")
                return@setOnClickListener
            }
            saveWithPin(pin)
        }
    }

    // ── Biometric / PIN flow ──────────────────────────────────────────────────

    private fun attemptBiometricSave() {
        val biometricEnabled = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("biometric_enabled", false)

        if (biometricEnabled
            && BiometricManager.isBiometricAvailable(this)
            && BiometricKeyStoreHelper.hasPinStored(this)
        ) {
            BiometricKeyStoreHelper.showDecryptPrompt(
                activity = this,
                onSuccess = { pin -> saveWithPin(pin) },
                onFailure  = { showManualPinEntry() }
            )
        } else {
            showManualPinEntry()
        }
    }

    private fun showManualPinEntry() {
        binding.layoutBiometricHint.visibility = View.GONE
        binding.etMasterPin.apply {
            visibility = View.VISIBLE
            inputType  = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        binding.btnSave.visibility = View.VISIBLE
    }

    // ── Vault save ────────────────────────────────────────────────────────────

    private fun saveWithPin(masterPin: String) {
        // Capture once — the getter hits FirebaseAuth on every call and could
        // theoretically return null mid-flight if the session is revoked.
        val uid = userId
        if (uid == null) {
            toast("Not logged in. Please open Security Angel and sign in first.")
            finish()
            return
        }

        setUiSaving(true)

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val salt = doc.getString("vaultSalt")
                if (salt.isNullOrEmpty()) {
                    toast("Vault not configured. Please open Security Angel once first.")
                    finish()
                    return@addOnSuccessListener
                }

                // PBKDF2 (100k iterations) must run off the main thread.
                lifecycleScope.launch {
                    val encPass = withContext(Dispatchers.IO) {
                        VaultCryptoManager.encrypt(password, masterPin, salt)
                    }
                    val encUser = withContext(Dispatchers.IO) {
                        VaultCryptoManager.encrypt(username, masterPin, salt)
                    }

                    val siteName = extractSiteName(domain)
                    val data = hashMapOf(
                        "siteName"  to siteName,
                        "searchKey" to siteName.lowercase(),
                        "domain"    to domain,
                        "email"     to encUser,
                        "password"  to encPass
                    )

                    // finish() is called ONLY inside the Firestore callbacks so the
                    // activity stays alive until the network round-trip completes.
                    // Calling finish() before .add() returns would destroy the process
                    // and abort the in-flight request before it reaches the server.
                    firestore.collection("users").document(uid)
                        .collection("vault")
                        .add(data)
                        .addOnSuccessListener {
                            // For the Credential Provider path, signal success back to
                            // the Credential Manager so it can update its internal state.
                            if (isFromCredentialProvider &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                            ) {
                                val resultData = Intent()
                                PendingIntentHandler.setCreateCredentialResponse(
                                    resultData,
                                    CreatePasswordResponse()
                                )
                                setResult(RESULT_OK, resultData)
                            }
                            toast("Password saved to Security Angel!")
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("AutofillSave", "Firestore write failed", e)
                            toast("Save failed: ${e.message}")
                            finish()
                        }
                }
            }
            .addOnFailureListener { e ->
                setUiSaving(false)
                toast("Connection error: ${e.message}")
            }
    }

    private fun setUiSaving(saving: Boolean) {
        binding.progressSave.visibility = if (saving) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled       = !saving
        binding.btnCancel.isEnabled     = !saving
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun extractSiteName(domain: String): String =
        domain.removePrefix("www.")
            .split(".")
            .firstOrNull()
            ?.replaceFirstChar { it.uppercase() }
            ?: domain

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
