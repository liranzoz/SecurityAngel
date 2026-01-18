package com.example.securityangel.utils

import okhttp3.*
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale

object BreachCheckUtil {

    private val client = OkHttpClient()

    fun checkPassword(password: String, onResult: (Boolean) -> Unit) {
        val sha1 = sha1Hash(password).uppercase(Locale.getDefault())
        val prefix = sha1.substring(0, 5)
        val suffix = sha1.substring(5)

        val request = Request.Builder()
            .url("https://api.pwnedpasswords.com/range/$prefix")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onResult(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onResult(false)
                        return
                    }
                    val responseBody = response.body?.string() ?: ""
                    val isLeaked = responseBody.contains(suffix)
                    onResult(isLeaked)
                }
            }
        })
    }

    private fun sha1Hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}