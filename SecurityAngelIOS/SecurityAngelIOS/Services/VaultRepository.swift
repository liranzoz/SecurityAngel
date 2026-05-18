import Foundation
import FirebaseFirestore

/// `users/{uid}/vault/{docId}` — encrypted credential storage.
///
/// All plaintext encryption happens here before writing to Firestore.
/// All decryption happens at read time on the caller's side via the
/// `decrypt(_:pin:salt:)` helper.
///
/// Documents are decoded manually from the raw dictionary rather than
/// through `Codable`. Swift's synthesized `Codable.init(from:)` throws
/// `keyNotFound` for any non-Optional field that is absent from the
/// document — and `id` is never present in Firestore docs (it lives as
/// the doc id). Manual decoding lets us fall back to defaults for any
/// missing field and never silently drops a row.
final class VaultRepository {

    private var db: Firestore { Firestore.firestore() }

    private func vault(for uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("vault")
    }

    // MARK: - Read

    func observe(uid: String, onChange: @escaping ([VaultEntry]) -> Void) -> ListenerRegistration {
        vault(for: uid).addSnapshotListener { snapshot, error in
            if let error {
                print("⚠️ VaultRepository.observe error: \(error.localizedDescription)")
                onChange([])
                return
            }
            guard let snapshot else {
                onChange([])
                return
            }
            let entries = snapshot.documents.compactMap(Self.decode(_:))
            onChange(entries)
        }
    }

    /// Manual decode. Robust to missing or extra fields.
    private static func decode(_ doc: QueryDocumentSnapshot) -> VaultEntry? {
        let d = doc.data()
        let entry = VaultEntry(
            id:        doc.documentID,
            searchKey: d["searchKey"] as? String ?? "",
            siteName:  d["siteName"]  as? String ?? "",
            email:     d["email"]     as? String ?? "",
            domain:    d["domain"]    as? String ?? "",
            password:  d["password"]  as? String ?? "",
            isLeaked:  d["isLeaked"]  as? Bool   ?? false
        )
        guard !entry.siteName.isEmpty || !entry.password.isEmpty else { return nil }
        return entry
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
