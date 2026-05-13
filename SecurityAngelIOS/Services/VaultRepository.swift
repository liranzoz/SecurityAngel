import Foundation
import FirebaseFirestore

/// `users/{uid}/vault/{docId}` — encrypted credential storage.
///
/// All plaintext encryption happens here before writing to Firestore.
/// All decryption happens at read time on the caller's side via the
/// `decrypt(_:pin:salt:)` helper.
final class VaultRepository {

    private var db: Firestore { Firestore.firestore() }

    private func vault(for uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("vault")
    }

    // MARK: - Read

    func observe(uid: String, onChange: @escaping ([VaultEntry]) -> Void) -> ListenerRegistration {
        vault(for: uid).addSnapshotListener { snapshot, _ in
            let entries = snapshot?.documents.compactMap { doc -> VaultEntry? in
                guard var entry = try? doc.data(as: VaultEntry.self) else { return nil }
                entry.id = doc.documentID
                return entry
            } ?? []
            onChange(entries)
        }
    }

    /// In-place decrypt of one entry. Caller supplies the active PIN + salt.
    /// Returns `("", "")` if either field can't be decrypted (matching the
    /// Android failure mode — wrong PIN yields empty strings).
    func decrypt(_ entry: VaultEntry, pin: String, salt: String) -> (email: String, password: String) {
        let email    = VaultCryptoManager.decrypt(base64Ciphertext: entry.email,    pin: pin, saltBase64: salt)
        let password = VaultCryptoManager.decrypt(base64Ciphertext: entry.password, pin: pin, saltBase64: salt)
        return (email, password)
    }

    // MARK: - Write

    func add(uid: String, siteName: String, domain: String, email: String, password: String, pin: String, salt: String) async throws {
        let encEmail    = try VaultCryptoManager.encrypt(plaintext: email,    pin: pin, saltBase64: salt)
        let encPassword = try VaultCryptoManager.encrypt(plaintext: password, pin: pin, saltBase64: salt)
        let data: [String: Any] = [
            "searchKey": siteName.lowercased(),
            "siteName":  siteName,
            "domain":    domain,
            "email":     encEmail,
            "password":  encPassword,
            "isLeaked":  false
        ]
        _ = try await vault(for: uid).addDocument(data: data)
    }

    func update(uid: String, entryId: String, siteName: String, domain: String, email: String, password: String, pin: String, salt: String) async throws {
        let encEmail    = try VaultCryptoManager.encrypt(plaintext: email,    pin: pin, saltBase64: salt)
        let encPassword = try VaultCryptoManager.encrypt(plaintext: password, pin: pin, saltBase64: salt)
        let data: [String: Any] = [
            "searchKey": siteName.lowercased(),
            "siteName":  siteName,
            "domain":    domain,
            "email":     encEmail,
            "password":  encPassword
        ]
        try await vault(for: uid).document(entryId).updateData(data)
    }

    func delete(uid: String, entryId: String) async throws {
        try await vault(for: uid).document(entryId).delete()
    }

    func markLeaked(uid: String, entryId: String, isLeaked: Bool) async throws {
        try await vault(for: uid).document(entryId).updateData(["isLeaked": isLeaked])
    }
}
