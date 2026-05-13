import Foundation
import FirebaseFirestore

/// `users/{uid}/chat_history/{id}` — assistant conversation history.
final class ChatHistoryRepository {

    private var db: Firestore { Firestore.firestore() }

    private func chats(for uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("chat_history")
    }

    func load(uid: String) async throws -> [ChatMessageDoc] {
        let snapshot = try await chats(for: uid)
            .order(by: "timestamp")
            .getDocuments()
        return snapshot.documents.compactMap { try? $0.data(as: ChatMessageDoc.self) }
    }

    func add(uid: String, message: ChatMessageDoc) async throws {
        _ = try chats(for: uid).addDocument(from: message)
    }
}
