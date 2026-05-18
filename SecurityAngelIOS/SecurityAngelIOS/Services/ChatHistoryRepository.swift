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
        return snapshot.documents.compactMap(Self.decode(_:))
    }

    func add(uid: String, message: ChatMessageDoc) async throws {
        var data: [String: Any] = [
            "text":      message.text,
            "isUser":    message.isUser,
            "timestamp": message.timestamp,
            "isLoading": message.isLoading
        ]
        if let imageUri = message.imageUri { data["imageUri"] = imageUri }
        _ = try await chats(for: uid).addDocument(data: data)
    }

    private static func decode(_ doc: QueryDocumentSnapshot) -> ChatMessageDoc? {
        let d = doc.data()
        let ts = (d["timestamp"] as? Int64) ?? Int64((d["timestamp"] as? Int) ?? 0)
        return ChatMessageDoc(
            id:        doc.documentID,
            text:      d["text"]      as? String ?? "",
            isUser:    d["isUser"]    as? Bool   ?? false,
            timestamp: ts,
            isLoading: d["isLoading"] as? Bool   ?? false,
            imageUri:  d["imageUri"]  as? String
        )
    }
}
