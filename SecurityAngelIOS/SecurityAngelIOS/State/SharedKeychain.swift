import Foundation
import Security

/// Resolves the shared Keychain Access Group string for both the main app
/// and the AutoFill extension. The string format is
/// `<TeamIdentifierPrefix>.<bundleSuffix>`. The team prefix isn't known at
/// build time, so we probe the keychain — every keychain item is tagged
/// with the app's current access group, and `kSecAttrAccessGroup` returns
/// `<TEAMID>.<defaultGroup>`. We split on the first dot to extract the
/// team prefix, then assemble our shared group identifier.
///
/// Both targets' entitlements declare:
///
///     keychain-access-groups = [ "$(AppIdentifierPrefix)com.zoz.SecurityAngelIOS" ]
///
/// so the same shared group string resolves identically in both processes.
enum SharedKeychain {

    /// The trailing portion of the shared keychain access group — the part
    /// after the team identifier prefix.
    static let suffix = "com.zoz.SecurityAngelIOS"

    /// Full resolved access group, e.g. `1234567890.com.zoz.SecurityAngelIOS`.
    /// Returns `nil` if the team prefix can't be determined.
    static var fullGroupID: String? {
        if let cached = cachedGroupID { return cached }

        let probeAccount = "_sa_team_id_probe"
        let query: [String: Any] = [
            kSecClass as String:        kSecClassGenericPassword,
            kSecAttrAccount as String:  probeAccount,
            kSecMatchLimit as String:   kSecMatchLimitOne,
            kSecReturnAttributes as String: true
        ]

        var result: AnyObject?
        var status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound {
            let addAttrs: [String: Any] = [
                kSecClass as String:       kSecClassGenericPassword,
                kSecAttrAccount as String: probeAccount,
                kSecValueData as String:   Data()
            ]
            SecItemAdd(addAttrs as CFDictionary, nil)
            status = SecItemCopyMatching(query as CFDictionary, &result)
        }

        guard status == errSecSuccess,
              let dict = result as? [String: Any],
              let accessGroup = dict[kSecAttrAccessGroup as String] as? String,
              let dotIndex = accessGroup.firstIndex(of: ".") else {
            return nil
        }
        let prefix = accessGroup[..<dotIndex]
        let full = "\(prefix).\(suffix)"
        cachedGroupID = full
        return full
    }

    private static var cachedGroupID: String?
}
