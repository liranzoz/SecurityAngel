package com.example.securityangel.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages a biometric-gated AES-GCM key in the Android KeyStore used to
 * protect the user's vault Master PIN at rest.  Both encrypt and decrypt
 * operations require a live biometric CryptoObject, so the plaintext PIN
 * never leaves the secure environment without user presence.
 */
object BiometricKeyStoreHelper {

    private const val KEYSTORE_PROVIDER  = "AndroidKeyStore"
    private const val KEY_ALIAS          = "sa_vault_pin_key"
    private const val TRANSFORMATION     = "AES/GCM/NoPadding"
    private const val GCM_TAG_LEN_BITS   = 128
    private const val PREFS_NAME         = "sa_biometric_vault"
    private const val KEY_ENCRYPTED_PIN  = "encrypted_pin"
    private const val KEY_IV             = "pin_iv"

    // ── KeyStore helpers ─────────────────────────────────────────────────────

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        kg.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return kg.generateKey()
    }

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun buildEncryptCipher(): Cipher {
        val key = getOrCreateKey()
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun buildDecryptCipher(context: Context): Cipher {
        val ivBytes = getStoredIv(context)
            ?: throw IllegalStateException("No IV stored — PIN was never protected via biometrics.")
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val key = ks.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("KeyStore key not found.")
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LEN_BITS, ivBytes))
        }
    }

    // ── SharedPreferences storage ─────────────────────────────────────────────

    fun storeEncryptedPin(cipher: Cipher, pin: String, context: Context) {
        val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ENCRYPTED_PIN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun decryptPin(cipher: Cipher, context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PIN, null) ?: return ""
        return String(cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun hasPinStored(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(KEY_ENCRYPTED_PIN)

    fun clearStoredPin(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        runCatching {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getStoredIv(context: Context): ByteArray? {
        val b64 = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IV, null) ?: return null
        return Base64.decode(b64, Base64.NO_WRAP)
    }

    // ── BiometricPrompt wrappers ──────────────────────────────────────────────

    /**
     * Shows a biometric prompt that encrypts [pin] with the KeyStore key on
     * successful authentication and persists the ciphertext.  Call this after
     * the user has manually entered their PIN (e.g. from PasswordVaultActivity).
     */
    fun showEncryptPrompt(
        activity: FragmentActivity,
        pin: String,
        onSuccess: () -> Unit,
        onCancel: () -> Unit
    ) {
        val cipher = runCatching { buildEncryptCipher() }.getOrElse {
            clearStoredPin(activity)
            onCancel()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher ?: return
                    storeEncryptedPin(authCipher, pin, activity)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onCancel()
                override fun onAuthenticationFailed() {}
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Protect Vault PIN")
            .setSubtitle("Securely store your PIN so future saves require only a fingerprint")
            .setNegativeButtonText("Skip")
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    /**
     * Shows a biometric prompt that decrypts the stored PIN on successful
     * authentication and delivers it to [onSuccess].  Falls back via [onFailure]
     * when the key was invalidated (new biometric enrollment) or auth was denied.
     */
    fun showDecryptPrompt(
        activity: FragmentActivity,
        title: String = "Confirm Save",
        subtitle: String = "Authenticate to save password to Security Angel",
        onSuccess: (pin: String) -> Unit,
        onFailure: () -> Unit
    ) {
        val cipher = try {
            buildDecryptCipher(activity)
        } catch (e: KeyPermanentlyInvalidatedException) {
            clearStoredPin(activity)
            onFailure()
            return
        } catch (e: Exception) {
            onFailure()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher ?: run { onFailure(); return }
                    val pin = decryptPin(authCipher, activity)
                    if (pin.isEmpty()) onFailure() else onSuccess(pin)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure()
                override fun onAuthenticationFailed() {}
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN instead")
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}
