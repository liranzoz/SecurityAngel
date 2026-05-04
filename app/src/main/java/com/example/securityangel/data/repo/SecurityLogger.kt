package com.example.securityangel.data.repo

import com.example.securityangel.data.models.SecurityLog
import com.example.securityangel.ui.notifactions.FcmNotificationSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SecurityLogger {

    private val db = FirebaseFirestore.getInstance()
    private val pushSender = FcmNotificationSender()

    const val TYPE_LEAK_FOUND = "LEAK_FOUND"
    const val TYPE_SCAN_SAFE = "SCAN_SAFE"
    const val TYPE_MALWARE = "MALWARE_DETECTED"
    const val TYPE_FAMILY_UPDATE = "MEMBER_ADDED"
    const val TYPE_PASS_GENERATED = "PASS_GENERATED"

    fun logEvent(eventType: String, title: String, description: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val familyId = document.getString("familyId")
                    val firstName = document.getString("firstName") ?: "Unknown"
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName".trim()

                    if (!familyId.isNullOrEmpty()) {
                        val logEntry = saveLogToFirebase(familyId, currentUserId, fullName, eventType, description)
                        handlePushNotifications(familyId, currentUserId, logEntry, title)
                    }
                }
            }
    }

    private fun handlePushNotifications(familyId: String, currentUserId: String, log: SecurityLog, title: String) {
        pushSender.sendPush(currentUserId, title, log.description)
        db.collection("families").document(familyId).get()
            .addOnSuccessListener { familyDoc ->
                val adminId = familyDoc.getString("adminId")

                if (adminId != null && adminId != currentUserId) {
                    val adminMsg = "$title (by ${log.userName})"
                    pushSender.sendPush(adminId, "Family Alert: $title", "User: ${log.userName}\n${log.description}")
                }
            }
    }

    private fun saveLogToFirebase(
        familyId: String,
        userId: String,
        userName: String,
        eventType: String,
        desc: String
    ): SecurityLog {
        val newLogRef = db.collection("security_logs").document()

        val log = SecurityLog(
            id = newLogRef.id,
            familyId = familyId,
            userId = userId,
            userName = userName,
            eventType = eventType,
            description = desc,
            timestamp = System.currentTimeMillis()
        )

        newLogRef.set(log)
        return log
    }
}
