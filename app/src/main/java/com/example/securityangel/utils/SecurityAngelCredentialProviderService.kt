package com.example.securityangel.utils

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.example.securityangel.R
import com.example.securityangel.ui.autofill.AutofillCredentialFillActivity
import com.example.securityangel.ui.autofill.AutofillSaveActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SecurityAngelCredentialProviderService : CredentialProviderService() {

    private val TAG = "AngelCreds"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created! The OS successfully connected to our provider.")
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        Log.d(TAG, "onBeginCreateCredentialRequest TRIGGERED! Credential Type: ${request.type}")

        when (request.type) {
            PasswordCredential.TYPE_PASSWORD_CREDENTIAL -> {
                Log.d(TAG, "Handling PasswordCredential creation request...")
                try {
                    val response = buildPasswordCreateResponse()
                    callback.onResult(response)
                    Log.d(TAG, "Success! Returned CreateEntry to the OS.")
                } catch (e: Exception) {
                    Log.e(TAG, "CRITICAL ERROR building response", e)
                    callback.onError(CreateCredentialUnknownException(e.message))
                }
            }
            else -> {
                Log.w(TAG, "Ignored unsupported credential type: ${request.type}")
                callback.onError(CreateCredentialUnknownException("Unsupported type"))
            }
        }
    }

    private fun buildPasswordCreateResponse(): BeginCreateCredentialResponse {
        val saveIntent = Intent(this, AutofillSaveActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AutofillSaveActivity.EXTRA_FROM_CREDENTIAL_PROVIDER, true)
        }

        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val pendingIntent = PendingIntent.getActivity(
            this, 2001, saveIntent, piFlags
        )

        val createEntry = CreateEntry.Builder(
            accountName = getString(R.string.app_name),
            pendingIntent = pendingIntent
        ).build()

        return BeginCreateCredentialResponse.Builder()
            .addCreateEntry(createEntry)
            .build()
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        Log.d(TAG, "onBeginGetCredentialRequest TRIGGERED")

        val passwordOption = request.beginGetCredentialOptions
            .filterIsInstance<BeginGetPasswordOption>()
            .firstOrNull()

        if (passwordOption == null) {
            callback.onResult(BeginGetCredentialResponse.Builder().build())
            return
        }

        // origin is set when the calling app verifies a web origin (browsers);
        // otherwise we fall back to the package name for native apps.
        val callingPackage = request.callingAppInfo?.packageName ?: ""
        val callingOrigin  = request.callingAppInfo?.origin ?: ""
        val domain = callingOrigin.ifEmpty { callingPackage }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d(TAG, "No signed-in user — returning empty response")
            callback.onResult(BeginGetCredentialResponse.Builder().build())
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users").document(userId).collection("vault")
            .get()
            .addOnSuccessListener { documents ->
                if (cancellationSignal.isCanceled) return@addOnSuccessListener
                try {
                    val responseBuilder = BeginGetCredentialResponse.Builder()
                    var matchCount = 0
                    for (doc in documents) {
                        val storedDomain = doc.getString("domain") ?: continue
                        if (!domainsMatch(domain, storedDomain)) continue
                        val siteName = doc.getString("siteName") ?: storedDomain

                        responseBuilder.addCredentialEntry(
                            buildCredentialEntry(doc.id, siteName, passwordOption)
                        )
                        matchCount++
                    }
                    Log.d(TAG, "Built $matchCount credential entries for domain=$domain")
                    callback.onResult(responseBuilder.build())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to build credential entries", e)
                    callback.onResult(BeginGetCredentialResponse.Builder().build())
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Vault query failed", e)
                callback.onResult(BeginGetCredentialResponse.Builder().build())
            }
    }

    private fun buildCredentialEntry(
        docId: String,
        siteName: String,
        passwordOption: BeginGetPasswordOption
    ): PasswordCredentialEntry {
        val fillIntent = Intent(this, AutofillCredentialFillActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AutofillCredentialFillActivity.EXTRA_DOC_ID, docId)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getActivity(
            this, docId.hashCode(), fillIntent, piFlags
        )
        return PasswordCredentialEntry.Builder(
            this,
            siteName,
            pendingIntent,
            passwordOption
        ).build()
    }

    private fun domainsMatch(current: String, stored: String): Boolean {
        if (current.isEmpty() || stored.isEmpty()) return false
        if (current.equals(stored, ignoreCase = true)) return true
        return rootDomain(current).equals(rootDomain(stored), ignoreCase = true)
    }

    private fun rootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) "${parts[parts.size - 2]}.${parts[parts.size - 1]}" else domain
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        Log.d(TAG, "onClearCredentialStateRequest TRIGGERED!")
        callback.onResult(null)
    }
}