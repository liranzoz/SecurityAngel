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
            .addSnapshotListener { snapshot, _ in
                let items = snapshot?.documents.compactMap { try? $0.data(as: ScanHistoryItem.self) } ?? []
                onChange(items)
            }
    }

    func mostRecent(uid: String) async throws -> ScanHistoryItem? {
        let snapshot = try await scans(for: uid)
            .order(by: "timestamp", descending: true)
            .limit(to: 1)
            .getDocuments()
        return try snapshot.documents.first?.data(as: ScanHistoryItem.self)
    }

    func save(uid: String, url: String, isSafe: Bool) async throws {
        let item = ScanHistoryItem(
            id: UUID().uuidString,
            url: url,
            status: isSafe ? "safe" : "unsafe",
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        _ = try scans(for: uid).addDocument(from: item)
    }
}
