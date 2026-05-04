package com.example.securityangel.ui.notifactions

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

class FcmNotificationSender {

    private val fcmApi: FcmApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        fcmApi = retrofit.create(FcmApi::class.java)
    }

    fun sendPush(userId: String, title: String, message: String) {
        val topic = "/topics/user_$userId"
        val notification = PushNotification(
            to = topic,
            notification = NotificationData(title, message)
        )

        fcmApi.sendNotification(notification).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    interface FcmApi {
        @Headers(
            "Content-Type: application/json",
            "Authorization: key=YOUR_SERVER_KEY_HERE"
        )
        @POST("fcm/send")
        fun sendNotification(@Body body: PushNotification): Call<Void>
    }

    data class PushNotification(
        val to: String,
        val notification: NotificationData
    )

    data class NotificationData(
        val title: String,
        val body: String
    )
}
