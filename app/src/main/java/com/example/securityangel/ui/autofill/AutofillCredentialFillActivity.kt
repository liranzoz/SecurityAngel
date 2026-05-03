package com.example.securityangel.ui.autofill

import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.lifecycleScope
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

/**
 * Handles the final step of the Credential Manager get-flow on Android 14+.
 * Activated when the user picks one of the entries our CredentialProviderService
 * returned from onBeginGetCredentialRequest.  Unlocks the vault if needed,
 * decrypts the requested entry, and returns it via PendingIntentHandler.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class AutofillCredentialFillActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DOC_ID = "credential_doc_id"
        private const val TAG = "AngelCredFill"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val userId get() = FirebaseAuth.getInstance().currentUser?.uid

    private var hasPrompted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val docId = intent.getStringExtra(EXTRA_DOC_ID)
        if (docId.isNullOrEmpty()) { cancelAndFinish(); return }

        if (VaultSessionManager.isValid) {
            decryptAndReturn(docId, VaultSessionManager.masterPin, VaultSessionManager.vaultSalt)
        }
        // Slow path is deferred to onResume so the Window is fully attached
        // before we can show the biometric prompt.
    }

    override fun onResume() {
        super.onResume()
        if (!hasPrompted && !VaultSessionManager.isValid) {
            hasPrompted = true
            fetchSaltThenUnlock()
        }
    }

    private fun fetchSaltThenUnlock() {
        val uid = userId ?: run { cancelAndFinish(); return }
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val salt = doc.getString("vaultSalt")
                if (salt.isNullOrEmpty()) { cancelAndFinish(); return@addOnSuccessListener }
                attemptUnlock(salt)
            }
            .addOnFailureListener { cancelAndFinish() }
    }

    private fun attemptUnlock(vaultSalt: String) {
        val biometricEnabled = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("biometric_enabled", false)

        if (biometricEnabled
            && BiometricManager.isBiometricAvailable(this)
            && BiometricKeyStoreHelper.hasPinStored(this)
        ) {
            BiometricKeyStoreHelper.showDecryptPrompt(
                activity  = this,
                title     = "Unlock Security Angel",
                subtitle  = "Authenticate to fill your credentials",
                onSuccess = { pin -> onPinObtained(pin, vaultSalt) },
                onFailure = { showPinDialog(vaultSalt) }
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
                if (pin.isNotEmpty()) onPinObtained(pin, vaultSalt) else cancelAndFinish()
            }
            .setNegativeButton("Cancel") { _, _ -> cancelAndFinish() }
            .show()
    }

    private fun onPinObtained(pin: String, vaultSalt: String) {
        VaultSessionManager.save(pin, vaultSalt)
        val docId = intent.getStringExtra(EXTRA_DOC_ID) ?: return cancelAndFinish()
        decryptAndReturn(docId, pin, vaultSalt)
    }

    private fun decryptAndReturn(docId: String, masterPin: String, vaultSalt: String) {
        val uid = userId ?: run { cancelAndFinish(); return }
        firestore.collection("users").document(uid)
            .collection("vault").document(docId).get()
            .addOnSuccessListener { doc ->
                lifecycleScope.launch {
                    val email = withContext(Dispatchers.IO) {
                        VaultCryptoManager.decrypt(doc.getString("email") ?: "", masterPin, vaultSalt)
                    }
                    val password = withContext(Dispatchers.IO) {
                        VaultCryptoManager.decrypt(doc.getString("password") ?: "", masterPin, vaultSalt)
                    }
                    if (password.isEmpty()) {
                        Log.w(TAG, "Decrypted password empty (wrong PIN or corrupt entry)")
                        cancelAndFinish()
                        return@launch
                    }
                    val resultIntent = android.content.Intent()
                    PendingIntentHandler.setGetCredentialResponse(
                        resultIntent,
                        GetCredentialResponse(PasswordCredential(email, password))
                    )
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
            .addOnFailureListener { cancelAndFinish() }
    }

    private fun cancelAndFinish() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
