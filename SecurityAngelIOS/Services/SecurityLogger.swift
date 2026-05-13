import Foundation
import FirebaseFirestore

/// `security_logs/{id}` + `users/{uid}` risk-count updates.
///
/// Mirrors Android's `SecurityLogger` + `SecurityRepository`. Push-notifying
/// the admin lives in Android's `FcmNotificationSender`; on iOS that will
/// require a Cloud Function in a later phase (the legacy FCM HTTP API the
/// Android app uses was deprecated mid-2024).
final class SecurityLogger {

    typealias EventType = SecurityLogDoc.EventType

    private var db: Firestore { Firestore.firestore() }
    private var logs: CollectionReference  { db.collection("security_logs") }
    private var users: CollectionReference { db.collection("users") }

    // MARK: - Realtime read

    func observeFamilyLogs(familyId: String, onChange: @escaping ([SecurityLogDoc]) -> Void) -> ListenerRegistration {
        logs.whereField("familyId", isEqualTo: familyId)
            .order(by: "timestamp", descending: true)
            .addSnapshotListener { snapshot, error in
                if let error {
                    print("⚠️ SecurityLogger.observe error: \(error.localizedDescription)")
                    onChange([])
                    return
                }
                let entries = snapshot?.documents.compactMap(Self.decode(_:)) ?? []
                onChange(entries)
            }
    }

    func clearFamilyLogs(familyId: String) async throws {
        let snapshot = try await logs.whereField("familyId", isEqualTo: familyId).getDocuments()
        let batch = db.batch()
        for doc in snapshot.documents { batch.deleteDocument(doc.reference) }
        try await batch.commit()
    }

    // MARK: - Write

    func logEvent(uid: String, eventType: EventType, description: String) async throws {
        let userSnap = try await users.document(uid).getDocument()
        guard let familyId = userSnap.get("familyId") as? String, !familyId.isEmpty else { return }
        let firstName = userSnap.get("firstName") as? String ?? "Unknown"
        let lastName  = userSnap.get("lastName")  as? String ?? ""
        let userName  = "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)

        let ref = logs.document()
        let data: [String: Any] = [
            "id":          ref.documentID,
            "familyId":    familyId,
            "userId":      uid,
            "userName":    userName,
            "eventType":   eventType.rawValue,
            "description": description,
            "timestamp":   Int64(Date().timeIntervalSince1970 * 1000)
        ]
        try await ref.setData(data)
    }

    // MARK: - Risk score

    func reportRisk(uid: String, risk: SecurityRisk) async throws {
        let docRef = users.document(uid)
        try await docRef.updateData([
            "activeRisks": FieldValue.arrayUnion([risk.rawValue]),
            "lastSecurityUpdate": Int64(Date().timeIntervalSince1970 * 1000)
        ])
        try await refreshRiskCount(uid: uid)
    }

    func resolveRisk(uid: String, risk: SecurityRisk) async throws {
        let docRef = users.document(uid)
        try await docRef.updateData([
            "activeRisks": FieldValue.arrayRemove([risk.rawValue]),
            "lastSecurityUpdate": Int64(Date().timeIntervalSince1970 * 1000)
        ])
        try await refreshRiskCount(uid: uid)
    }

    private func refreshRiskCount(uid: String) async throws {
        let snap = try await users.document(uid).getDocument()
        let risks = snap.get("activeRisks") as? [String] ?? []
        try await users.document(uid).updateData(["riskCount": risks.count])
    }

    // MARK: - Decode

    private static func decode(_ doc: QueryDocumentSnapshot) -> SecurityLogDoc? {
        let d = doc.data()
        let ts = (d["timestamp"] as? Int64) ?? Int64((d["timestamp"] as? Int) ?? 0)
        return SecurityLogDoc(
            id:          doc.documentID,
            familyId:    d["familyId"]    as? String ?? "",
            userId:      d["userId"]      as? String ?? "",
            userName:    d["userName"]    as? String ?? "",
            eventType:   d["eventType"]   as? String ?? "",
            description: d["description"] as? String ?? "",
            timestamp:   ts
        )
    }
}
