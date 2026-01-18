package com.example.securityangel.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val SECRET_KEY = "MokedMeitav2025!"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun encrypt(value: String): String {
        try {
            val key = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
            val iv = IvParameterSpec(ByteArray(16)) // וקטור איפוס פשוט
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            val encrypted = cipher.doFinal(value.toByteArray())
            return Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return value
        }
    }

    fun decrypt(encrypted: String): String {
        try {
            val key = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
            val iv = IvParameterSpec(ByteArray(16))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
            return String(original)
        } catch (e: Exception) {
            return ""
        }
    }
}