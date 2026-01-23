package com.example.securityangel.utils

import android.util.Base64
import com.example.securityangel.BuildConfig
import com.example.securityangel.data.models.VtResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VirusTotalApi {

    // 1. קבלת דוח סופי של לינק
    @GET("api/v3/urls/{id}")
    fun scanUrl(@Path("id") urlId: String): Call<VtResponse>

    // 2. שליחת לינק חדש לסריקה -> מחזיר Analysis ID
    @FormUrlEncoded
    @POST("api/v3/urls")
    fun submitUrlForScanning(@Field("url") url: String): Call<VtResponse>

    // 3. בדיקת סטטוס של סריקה שרצה כרגע
    @GET("api/v3/analyses/{id}")
    fun getAnalysisStatus(@Path("id") analysisId: String): Call<VtResponse>

    companion object {
        private const val BASE_URL = "https://www.virustotal.com/"

        fun create(): VirusTotalApi {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest: Request = originalRequest.newBuilder()
                    .addHeader("x-apikey", BuildConfig.VIRUSTOTAL_API_KEY)
                    .build()
                chain.proceed(newRequest)
            }.build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VirusTotalApi::class.java)
        }

        fun urlToBase64(url: String): String {
            return Base64.encodeToString(
                url.toByteArray(),
                Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
            )
        }
    }
}