import Foundation
import FirebaseFirestore

/// Firestore `users/{uid}` collection access. Mirrors the user shape used
/// by `BaseActivity.fetchUserDetails` on Android.
///
/// Decoding is manual (dictionary access). Swift's synthesized
/// `Codable.init(from:)` throws on any non-Optional field that's missing
/// from the document, which is fatal for tolerant cross-platform sync.
final class UserRepository {

    private var users: CollectionReference {
        Firestore.firestore().collection("users")
    }

    // MARK: - Single fetch

    func get(id: String) async throws -> SecurityUser? {
        let snapshot = try await users.document(id).getDocument()
        return Self.decode(snapshot)
    }

    // MARK: - Write

    /// Used after sign-up to create the user document. Stores via plain
    /// dictionary so we don't fight Codable on the way out either.
    func create(_ user: SecurityUser) async throws {
        try await users.document(user.id).setData(Self.encode(user))
    }

    func update(id: String, fields: [String: Any]) async throws {
        try await users.document(id).updateData(fields)
    }

    // MARK: - Vault salt bootstrap

    /// Returns the user's PBKDF2 salt, generating + persisting a new one
    /// if missing. Equivalent to `DashboardActivity.ensureVaultSaltExists`.
    func ensureVaultSalt(uid: String) async throws -> String {
        let doc = users.document(uid)
        let snapshot = try await doc.getDocument()
        if let existing = snapshot.get("vaultSalt") as? String, !existing.isEmpty {
            return existing
        }
        let newSalt = VaultCryptoManager.generateSalt()
        try await doc.updateData(["vaultSalt": newSalt])
        return newSalt
    }

    // MARK: - Realtime

    func observe(id: String, onChange: @escaping (SecurityUser?) -> Void) -> ListenerRegistration {
        users.document(id).addSnapshotListener { snapshot, error in
            if let error {
                print("⚠️ UserRepository.observe(\(id)) error: \(error.localizedDescription)")
                onChange(nil)
                return
            }
            onChange(Self.decode(snapshot))
        }
    }

    /// Looks up another user by email (used by the family invitation flow).
    func find(byEmail email: String) async throws -> SecurityUser? {
        let snapshot = try await users.whereField("email", isEqualTo: email).limit(to: 1).getDocuments()
        guard let first = snapshot.documents.first else { return nil }
        return Self.decode(first)
    }

    // MARK: - Helpers

    static func decode(_ snapshot: DocumentSnapshot?) -> SecurityUser? {
        guard let snapshot, snapshot.exists, let d = snapshot.data() else { return nil }
        return SecurityUser(
            id:             snapshot.documentID,
            firstName:      d["firstName"] as? String ?? "",
            lastName:       d["lastName"]  as? String ?? "",
            email:          d["email"]     as? String ?? "",
            phone:          d["phone"]     as? String ?? "",
            gender:         d["gender"]    as? String ?? "",
            familyId:       (d["familyId"] as? String).flatMap { $0.isEmpty ? nil : $0 },
            securityStatus: d["securityStatus"] as? String ?? "Safe",
            riskCount:      (d["riskCount"] as? Int) ?? Int(d["riskCount"] as? Int64 ?? 0),
            lastScanDate:   (d["lastScanDate"] as? Int64) ?? Int64((d["lastScanDate"] as? Int) ?? 0),
            securityScore:  (d["securityScore"] as? Int) ?? Int(d["securityScore"] as? Int64 ?? 100),
            activeRisks:    d["activeRisks"] as? [String] ?? [],
            vaultSalt:      d["vaultSalt"] as? String
        )
    }

    static func encode(_ user: SecurityUser) -> [String: Any] {
        var data: [String: Any] = [
            "id":             user.id,
            "firstName":      user.firstName,
            "lastName":       user.lastName,
            "email":          user.email,
            "phone":          user.phone,
            "gender":         user.gender,
            "securityStatus": user.securityStatus,
            "riskCount":      user.riskCount,
            "lastScanDate":   user.lastScanDate,
            "securityScore":  user.securityScore,
            "activeRisks":    user.activeRisks
        ]
        if let familyId = user.familyId, !familyId.isEmpty { data["familyId"] = familyId }
        if let salt = user.vaultSalt, !salt.isEmpty { data["vaultSalt"] = salt }
        return data
    }
}
