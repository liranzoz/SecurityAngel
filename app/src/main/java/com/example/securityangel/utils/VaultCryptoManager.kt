package com.example.securityangel.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCryptoManager {

    private const val PBKDF2_ALGO  = "PBKDF2WithHmacSHA256"
    private const val AES_GCM_ALGO = "AES/GCM/NoPadding"
    private const val KEY_LEN_BITS = 256
    private const val ITERATIONS   = 100_000
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES     = 12
    private const val SALT_BYTES   = 16

    // Single-entry cache so PBKDF2 runs at most once per (pin, salt) pair per session.
    private var cachedPin: String = ""
    private var cachedSalt: String = ""
    private var cachedKeyBytes: ByteArray = ByteArray(0)

    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun deriveKey(masterPin: String, saltBase64: String): SecretKeySpec {
        if (masterPin == cachedPin && saltBase64 == cachedSalt && cachedKeyBytes.isNotEmpty()) {
            return SecretKeySpec(cachedKeyBytes, "AES")
        }
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(masterPin.toCharArray(), salt, ITERATIONS, KEY_LEN_BITS)
        val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).encoded
        spec.clearPassword()
        cachedPin = masterPin
        cachedSalt = saltBase64
        cachedKeyBytes = keyBytes
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String, masterPin: String, saltBase64: String): String {
        val key = deriveKey(masterPin, saltBase64)
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, masterPin: String, saltBase64: String): String {
        return try {
            val combined   = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv         = combined.sliceArray(0 until IV_BYTES)
            val cipherText = combined.sliceArray(IV_BYTES until combined.size)
            val key = deriveKey(masterPin, saltBase64)
            val cipher = Cipher.getInstance(AES_GCM_ALGO)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}