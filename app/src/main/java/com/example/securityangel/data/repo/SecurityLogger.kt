package com.example.securityangel.data.repo

import com.example.securityangel.data.models.SecurityLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SecurityLogger {

    private val db = FirebaseFirestore.getInstance()

    const val TYPE_LEAK_FOUND = "LEAK_FOUND"
    const val TYPE_SCAN_SAFE = "SCAN_SAFE"
    const val TYPE_MALWARE = "MALWARE_DETECTED"
    const val TYPE_FAMILY_UPDATE = "MEMBER_ADDED"


    fun logEvent(eventType: String, description: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val familyId = document.getString("familyId")
                    val firstName = document.getString("firstName") ?: "Unknown"
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName".trim()

                    if (!familyId.isNullOrEmpty()) {
                        saveLogToFirebase(familyId, currentUser.uid, fullName, eventType, description)
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun saveLogToFirebase(
        familyId: String,
        userId: String,
        userName: String,
        eventType: String,
        desc: String
    ) {
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
    }
}