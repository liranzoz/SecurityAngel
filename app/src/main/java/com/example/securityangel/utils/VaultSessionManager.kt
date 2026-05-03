package com.example.securityangel.utils

/**
 * Process-lifetime cache for the unlocked vault credentials.
 *
 * Storing the PIN and salt in memory means returning to PasswordVaultActivity
 * within the same app session never re-prompts the user.  The cache is cleared
 * on every sign-out so a different user opening the app starts clean.
 *
 * Security note: this lives only in the JVM heap — it is never written to disk,
 * SharedPreferences, or any persistent store.  Android kills the process on
 * memory pressure or when the app is removed from recents, which wipes it
 * automatically.  The KeyStore-backed ciphertext in BiometricKeyStoreHelper is
 * the only persistent secret, and it is never in plaintext on disk.
 */
object VaultSessionManager {

    private var _masterPin: String = ""
    private var _vaultSalt: String = ""

    val masterPin: String get() = _masterPin
    val vaultSalt: String get() = _vaultSalt

    /** True when both pin and salt are populated from a successful unlock. */
    val isValid: Boolean get() = _masterPin.isNotEmpty() && _vaultSalt.isNotEmpty()

    fun save(pin: String, salt: String) {
        _masterPin = pin
        _vaultSalt = salt
    }

    /** Must be called on every sign-out path so a new session starts clean. */
    fun clear() {
        _masterPin = ""
        _vaultSalt = ""
    }
}
