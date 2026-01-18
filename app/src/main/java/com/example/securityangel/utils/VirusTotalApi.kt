package com.example.securityangel.utils

import android.util.Base64
import com.example.securityangel.data.models.VtResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface VirusTotalApi {

    @GET("api/v3/urls/{id}")
    fun scanUrl(
        @Header("x-apikey") apiKey: String,
        @Path("id") urlId: String
    ): Call<VtResponse>

    companion object {
        private const val BASE_URL = "https://www.virustotal.com/"

        fun create(): VirusTotalApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VirusTotalApi::class.java)
        }

        fun urlToBase64(url: String): String {
            return Base64.encodeToString(url.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }
}