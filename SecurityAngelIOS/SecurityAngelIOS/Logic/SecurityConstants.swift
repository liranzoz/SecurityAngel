import Foundation

/// Mirrors Android's `SecurityConstants.kt`. Used as keys in the user
/// document's `activeRisks` field.
enum SecurityRisk: String {
    case passwordLeak = "risk_password_leak"
    case maliciousApp = "risk_malicious_app"
    case unsecureWifi = "risk_unsecure_wifi"
    case outdatedOS   = "risk_outdated_os"
    case jailbroken   = "risk_jailbroken_device"
}

enum SecurityStatus: String {
    case safe   = "safe"
    case atRisk = "at_risk"
}
