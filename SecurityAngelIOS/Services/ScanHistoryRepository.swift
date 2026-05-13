import Foundation
import FirebaseFirestore

/// `users/{uid}/scans/{id}` — URL scan history for the recent-scans list.
final class ScanHistoryRepository {

    private var db: Firestore { Firestore.firestore() }

    private func scans(for uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("scans")
    }

    func observeRecent(uid: String, limit: Int = 5, onChange: @escaping ([ScanHistoryItem]) -> Void) -> ListenerRegistration {
        scans(for: uid)
            .order(by: "timestamp", descending: true)
            .limit(to: limit)
            .addSnapshotListener { snapshot, error in
                if let error {
                    print("⚠️ ScanHistoryRepository.observe error: \(error.localizedDescription)")
                    onChange([])
                    return
                }
                let items = snapshot?.documents.compactMap(Self.decode(_:)) ?? []
                onChange(items)
            }
    }

    func mostRecent(uid: String) async throws -> ScanHistoryItem? {
        let snapshot = try await scans(for: uid)
            .order(by: "timestamp", descending: true)
            .limit(to: 1)
            .getDocuments()
        return snapshot.documents.first.flatMap(Self.decode(_:))
    }

    func save(uid: String, url: String, isSafe: Bool) async throws {
        let data: [String: Any] = [
            "url":       url,
            "status":    isSafe ? "safe" : "unsafe",
            "timestamp": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        _ = try await scans(for: uid).addDocument(data: data)
    }

    private static func decode(_ doc: QueryDocumentSnapshot) -> ScanHistoryItem? {
        let d = doc.data()
        let ts = (d["timestamp"] as? Int64) ?? Int64((d["timestamp"] as? Int) ?? 0)
        return ScanHistoryItem(
            id:        doc.documentID,
            url:       d["url"]    as? String ?? "",
            status:    d["status"] as? String ?? "",
            timestamp: ts
        )
    }
}
