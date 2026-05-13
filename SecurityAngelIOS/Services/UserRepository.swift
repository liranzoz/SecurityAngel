import Foundation
import FirebaseFirestore

/// Firestore `users/{uid}` collection access. Mirrors the user shape used
/// by `BaseActivity.fetchUserDetails` on Android.
final class UserRepository {

    private var users: CollectionReference {
        Firestore.firestore().collection("users")
    }

    // MARK: - Single fetch

    func get(id: String) async throws -> SecurityUser? {
        let snapshot = try await users.document(id).getDocument()
        return try Self.decode(snapshot)
    }

    // MARK: - Write

    /// Used after sign-up to create the user document. The Firestore
    /// document id is the Firebase Auth UID; we set it explicitly so the
    /// embedded `id` field matches.
    func create(_ user: SecurityUser) async throws {
        var toSave = user
        try users.document(user.id).setData(from: toSave)
        _ = toSave
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
        users.document(id).addSnapshotListener { snapshot, _ in
            onChange((try? Self.decode(snapshot)) ?? nil)
        }
    }

    /// Looks up another user by email (used by the family invitation flow).
    func find(byEmail email: String) async throws -> SecurityUser? {
        let snapshot = try await users.whereField("email", isEqualTo: email).limit(to: 1).getDocuments()
        guard let first = snapshot.documents.first else { return nil }
        return try Self.decode(first)
    }

    // MARK: - Helpers

    private static func decode(_ snapshot: DocumentSnapshot?) throws -> SecurityUser? {
        guard let snapshot, snapshot.exists else { return nil }
        var user = try snapshot.data(as: SecurityUser.self)
        user.id = snapshot.documentID
        return user
    }
}
