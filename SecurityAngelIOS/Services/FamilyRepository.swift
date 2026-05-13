import Foundation
import FirebaseFirestore

/// Family CRUD + member management. Mirrors Android's `FamilyRepository`.
final class FamilyRepository {

    private var db: Firestore { Firestore.firestore() }
    private var families: CollectionReference { db.collection("families") }
    private var users: CollectionReference { db.collection("users") }

    enum FamilyError: LocalizedError {
        case userNotFound
        case alreadyInFamily
        case familyNotFound

        var errorDescription: String? {
            switch self {
            case .userNotFound:     return "User with this email not found."
            case .alreadyInFamily:  return "User already belongs to a family."
            case .familyNotFound:   return "Family not found."
            }
        }
    }

    // MARK: - Create

    func createFamily(admin: SecurityUser, name: String) async throws -> String {
        let ref = families.document()
        let family = SecurityFamily(
            id: ref.documentID,
            adminId: admin.id,
            name: name,
            members: [admin.id]
        )
        let batch = db.batch()
        try batch.setData(from: family, forDocument: ref)
        batch.updateData(["familyId": ref.documentID], forDocument: users.document(admin.id))
        try await batch.commit()
        return ref.documentID
    }

    // MARK: - Add / remove members

    func addMember(familyId: String, byEmail email: String) async throws {
        let lookup = try await users.whereField("email", isEqualTo: email).limit(to: 1).getDocuments()
        guard let doc = lookup.documents.first else { throw FamilyError.userNotFound }
        if let existing = doc.get("familyId") as? String, !existing.isEmpty {
            throw FamilyError.alreadyInFamily
        }
        let memberId = doc.documentID
        let batch = db.batch()
        batch.updateData(
            ["members": FieldValue.arrayUnion([memberId])],
            forDocument: families.document(familyId)
        )
        batch.updateData(["familyId": familyId], forDocument: users.document(memberId))
        try await batch.commit()
    }

    func removeMember(familyId: String, memberId: String) async throws {
        let batch = db.batch()
        batch.updateData(
            ["members": FieldValue.arrayRemove([memberId])],
            forDocument: families.document(familyId)
        )
        batch.updateData(["familyId": FieldValue.delete()], forDocument: users.document(memberId))
        try await batch.commit()
    }

    // MARK: - Read

    func get(id: String) async throws -> SecurityFamily? {
        let snapshot = try await families.document(id).getDocument()
        guard snapshot.exists else { return nil }
        var family = try snapshot.data(as: SecurityFamily.self)
        family.id = snapshot.documentID
        return family
    }

    func observeMembers(familyId: String, onChange: @escaping ([SecurityUser]) -> Void) -> ListenerRegistration {
        users.whereField("familyId", isEqualTo: familyId).addSnapshotListener { snapshot, _ in
            let members = snapshot?.documents.compactMap { snap -> SecurityUser? in
                guard var u = try? snap.data(as: SecurityUser.self) else { return nil }
                u.id = snap.documentID
                return u
            } ?? []
            onChange(members)
        }
    }

    // MARK: - Join via invitation

    /// Atomically attach `userId` to `familyId` and remove the invitation.
    func joinFamily(userId: String, familyId: String, invitationEmail: String) async throws {
        let batch = db.batch()
        batch.updateData(["familyId": familyId], forDocument: users.document(userId))
        batch.updateData(
            ["members": FieldValue.arrayUnion([userId])],
            forDocument: families.document(familyId)
        )
        batch.deleteDocument(db.collection("invitations").document(invitationEmail))
        try await batch.commit()
    }
}
