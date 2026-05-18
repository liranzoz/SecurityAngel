package com.example.securityangel.data.repo

import com.example.securityangel.data.models.SecurityLog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object SecurityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")
    private val logsRef = db.collection("security_logs")

    fun reportRisk(userId: String, riskType: String, onSuccess: () -> Unit = {}) {
        usersRef.document(userId).update(
            mapOf(
                "activeRisks" to FieldValue.arrayUnion(riskType),
                "lastSecurityUpdate" to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            updateRiskCount(userId)
            onSuccess()
        }
    }

    fun resolveRisk(userId: String, riskType: String, onSuccess: () -> Unit = {}) {
        usersRef.document(userId).update(
            mapOf(
                "activeRisks" to FieldValue.arrayRemove(riskType),
                "lastSecurityUpdate" to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            updateRiskCount(userId)
            onSuccess()
        }
    }

    private fun updateRiskCount(userId: String) {
        usersRef.document(userId).get().addOnSuccessListener { doc ->
            val risks = doc.get("activeRisks") as? List<String> ?: emptyList()
            usersRef.document(userId).update("riskCount", risks.size)
        }
    }

    fun logEvent(familyId: String, userId: String, userName: String, type: String, desc: String) {
        if (familyId.isEmpty()) return

        val newLog = SecurityLog(
            id = logsRef.document().id,
            familyId = familyId,
            userId = userId,
            userName = userName,
            eventType = type,
            description = desc,
            timestamp = System.currentTimeMillis()
        )
        logsRef.document(newLog.id).set(newLog)
    }

    fun getFamilyLogs(familyId: String, onSuccess: (List<SecurityLog>) -> Unit) {
        logsRef.whereEqualTo("familyId", familyId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null || value == null) return@addSnapshotListener
                val logs = value.toObjects(SecurityLog::class.java)
                onSuccess(logs)
            }
    }

    fun clearFamilyLogs(familyId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        logsRef.whereEqualTo("familyId", familyId).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.message ?: "Failed to clear logs") }
            }
            .addOnFailureListener { onFailure(it.message ?: "Failed to fetch logs") }
    }

}
