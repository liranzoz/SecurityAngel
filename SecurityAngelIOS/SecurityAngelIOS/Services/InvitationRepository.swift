import Foundation
import FirebaseFirestore

/// `invitations/{email}` — the family-invite codes. Keyed by invitee email
/// (matches the Android schema exactly).
final class InvitationRepository {

    private var collection: CollectionReference {
        Firestore.firestore().collection("invitations")
    }

    /// Generate a 6-digit code, persist the invitation, return the code.
    func create(email: String, familyId: String) async throws -> String {
        let code = String(format: "%06d", Int.random(in: 100_000...999_999))
        let data: [String: Any] = [
            "email":     email,
            "familyId":  familyId,
            "code":      code,
            "status":    "pending",
            "timestamp": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        try await collection.document(email).setData(data)
        return code
    }

    /// Verify a join attempt. Returns the family id on match, `nil` on
    /// invalid code or missing invitation.
    func verify(email: String, code: String) async throws -> String? {
        let snapshot = try await collection.document(email).getDocument()
        guard snapshot.exists, let d = snapshot.data() else { return nil }
        let storedCode = d["code"] as? String ?? ""
        let familyId   = d["familyId"] as? String ?? ""
        guard storedCode == code, !familyId.isEmpty else { return nil }
        return familyId
    }
}
