package com.example.securityangel.data.repo

import com.example.securityangel.data.models.Family
import com.example.securityangel.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object FamilyRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")
    private val familiesRef = db.collection("families")

    fun createFamily(admin: User, familyName: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val newFamilyRef = familiesRef.document()
        val familyId = newFamilyRef.id

        val newFamily = Family(
            id = familyId,
            adminId = admin.id,
            name = familyName,
            members = listOf(admin.id)
        )

        db.runBatch { batch ->
            batch.set(newFamilyRef, newFamily)
            batch.update(usersRef.document(admin.id), "familyId", familyId)
        }.addOnSuccessListener {
            onSuccess(familyId)
        }.addOnFailureListener { e ->
            onFailure(e.message ?: "Error creating family")
        }
    }

    fun addMemberByEmail(familyId: String, email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {

        usersRef.whereEqualTo("email", email).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onFailure("User with this email not found")
                    return@addOnSuccessListener
                }

                val userDoc = snapshot.documents.first()
                val userId = userDoc.id

                if (userDoc.getString("familyId") != null) {
                    onFailure("User already belongs to a family")
                    return@addOnSuccessListener
                }

                db.runBatch { batch ->
                    batch.update(familiesRef.document(familyId), "members", FieldValue.arrayUnion(userId))
                    batch.update(usersRef.document(userId), "familyId", familyId)
                }.addOnSuccessListener {

                    onSuccess()
                }.addOnFailureListener { e ->

                    onFailure(e.message ?: "Failed to add member")
                }
            }
            .addOnFailureListener {
                onFailure("Error searching for user")
            }
    }

    fun getFamilyMembers(familyId: String, onResult: (List<User>) -> Unit) {
        usersRef.whereEqualTo("familyId", familyId).get()
            .addOnSuccessListener { snapshot ->
                val members = snapshot.toObjects(User::class.java)
                onResult(members)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun getFamilyData(familyId: String, onSuccess: (Family) -> Unit, onFailure: (String) -> Unit) {
        familiesRef.document(familyId).get()
            .addOnSuccessListener { document ->
                val family = document.toObject(Family::class.java)
                if (family != null) {
                    onSuccess(family)
                } else {
                    onFailure("Family not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Error fetching family")
            }
    }

    fun removeMember(familyId: String, memberId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.runBatch { batch ->
            batch.update(familiesRef.document(familyId), "members", FieldValue.arrayRemove(memberId))

            batch.update(usersRef.document(memberId), "familyId", null)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e.message ?: "Failed to remove member")
        }
    }
}
