import Foundation

// MARK: - Canonical models — Codable, mapping 1:1 to Firestore docs.
// Field names exactly match the Android `data class` definitions so the
// same Firestore project works across both platforms.

/// users/{uid}
struct SecurityUser: Codable, Identifiable, Hashable {
    var id: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var phone: String = ""
    var gender: String = ""

    var familyId: String? = nil
    var securityStatus: String = "Safe"
    var riskCount: Int = 0
    var lastScanDate: Int64 = 0
    var securityScore: Int = 100
    var activeRisks: [String] = []
    var vaultSalt: String? = nil      // base64 PBKDF2 salt
}

extension SecurityUser {
    var fullName: String { "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces) }

    var avatarSymbol: String {
        switch gender {
        case "Male":   return "person.crop.circle.fill"
        case "Female": return "person.crop.circle.badge.heart"
        default:       return "person.crop.circle"
        }
    }
}

/// families/{familyId}
struct SecurityFamily: Codable, Identifiable, Hashable {
    var id: String = ""
    var adminId: String = ""
    var name: String = ""
    var members: [String] = []
}

/// users/{uid}/vault/{docId}
/// `email` and `password` are AES-GCM ciphertexts (base64). `searchKey`,
/// `siteName`, `domain` are plaintext (server-side queries need them).
struct VaultEntry: Codable, Identifiable, Hashable {
    var id: String = ""
    var searchKey: String = ""
    var siteName: String = ""
    var email: String = ""        // ciphertext
    var domain: String = ""
    var password: String = ""     // ciphertext
    var isLeaked: Bool = false
}

/// users/{uid}/scans/{docId}
struct ScanHistoryItem: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString
    var url: String = ""
    var status: String = ""        // "safe" | "unsafe"
    var timestamp: Int64 = 0

    var date: Date { Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0) }
    var isSafe: Bool { status == "safe" }
}

/// users/{uid}/chat_history/{docId}
struct ChatMessageDoc: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString
    var text: String = ""
    var isUser: Bool = false
    var timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    var isLoading: Bool = false
    var imageUri: String? = nil
}

/// security_logs/{logId}
struct SecurityLogDoc: Codable, Identifiable, Hashable {
    enum EventType: String, Codable {
        case leakFound      = "LEAK_FOUND"
        case scanSafe       = "SCAN_SAFE"
        case malware        = "MALWARE_DETECTED"
        case memberAdded    = "MEMBER_ADDED"
        case passGenerated  = "PASS_GENERATED"
    }

    var id: String = ""
    var familyId: String = ""
    var userId: String = ""
    var userName: String = ""
    var eventType: String = ""
    var description: String = ""
    var timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)

    var date: Date { Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0) }
    var typedEvent: EventType? { EventType(rawValue: eventType) }
}

/// users/{uid}/permissions_risks/latest
struct DevicePostureSnapshot: Codable, Hashable {
    struct RootStatus: Codable, Hashable {
        var isRooted: Bool = false
        var suBinariesFound: Bool = false
        var testKeysFound: Bool = false
        var rootPackagesFound: Bool = false
    }
    struct RiskyAppEntry: Codable, Hashable {
        var packageName: String
        var appName: String
        var riskLevel: String       // "HIGH" | "MEDIUM" | "LOW" | "SAFE"
        var riskExplanation: String
        var sensitivePermissionsSummary: String
    }

    var rootStatus: RootStatus = .init()
    var riskyApps: [RiskyAppEntry] = []
    var lastScanTimestamp: Int64 = 0
}

/// invitations/{email}
struct FamilyInvitation: Codable, Hashable {
    var email: String
    var familyId: String
    var code: String          // 6-digit
    var status: String        // "pending"
    var timestamp: Int64
}
