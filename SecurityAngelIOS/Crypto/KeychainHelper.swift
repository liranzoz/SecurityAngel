import Foundation
import LocalAuthentication
import Security

/// Equivalent of Android's `BiometricKeyStoreHelper`:
///
/// - Stores the user's master Vault PIN in the iOS Keychain.
/// - The Keychain entry's access control requires `.biometryCurrentSet`, so
///   it can only be read after Face/Touch ID succeeds and the PIN is
///   automatically invalidated when the user re-enrolls biometrics.
enum KeychainHelper {

    private static let service = "com.zoz.SecurityAngelIOS.vaultPIN"
    private static let account = "vault_master_pin"

    enum KeychainError: Error {
        case unexpectedStatus(OSStatus)
        case accessControlCreationFailed
        case authenticationFailed
        case noStoredPIN
    }

    // MARK: - Capability

    static var biometricsAvailable: Bool {
        var error: NSError?
        let ctx = LAContext()
        return ctx.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }

    static var hasStoredPIN: Bool {
        var query: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecMatchLimit as String:  kSecMatchLimitOne,
            kSecReturnAttributes as String: false,
            kSecUseAuthenticationUI as String: kSecUseAuthenticationUIFail
        ]
        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
        return status == errSecSuccess || status == errSecInteractionNotAllowed
    }

    // MARK: - Write

    /// Store the user's master PIN. The OS will prompt for biometric
    /// authentication before unlocking it on future reads.
    static func storePIN(_ pin: String) throws {
        guard let pinData = pin.data(using: .utf8) else { throw KeychainError.unexpectedStatus(errSecParam) }

        var error: Unmanaged<CFError>?
        guard let access = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
            .biometryCurrentSet,
            &error
        ) else {
            throw KeychainError.accessControlCreationFailed
        }

        let attributes: [String: Any] = [
            kSecClass as String:           kSecClassGenericPassword,
            kSecAttrService as String:     service,
            kSecAttrAccount as String:     account,
            kSecValueData as String:       pinData,
            kSecAttrAccessControl as String: access
        ]

        SecItemDelete([
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ] as CFDictionary)

        let status = SecItemAdd(attributes as CFDictionary, nil)
        guard status == errSecSuccess else { throw KeychainError.unexpectedStatus(status) }
    }

    // MARK: - Read

    /// Read the stored PIN, prompting biometrics. Returns the PIN string on
    /// success; throws on cancel/error.
    static func readPIN(promptReason: String = "Unlock your Security Angel vault") async throws -> String {
        let context = LAContext()
        context.localizedReason = promptReason
        context.touchIDAuthenticationAllowableReuseDuration = 10

        let query: [String: Any] = [
            kSecClass as String:           kSecClassGenericPassword,
            kSecAttrService as String:     service,
            kSecAttrAccount as String:     account,
            kSecReturnData as String:      true,
            kSecMatchLimit as String:      kSecMatchLimitOne,
            kSecUseAuthenticationContext as String: context
        ]

        return try await withCheckedThrowingContinuation { (cont: CheckedContinuation<String, Error>) in
            DispatchQueue.global(qos: .userInitiated).async {
                var dataRef: AnyObject?
                let status = SecItemCopyMatching(query as CFDictionary, &dataRef)
                switch status {
                case errSecSuccess:
                    if let data = dataRef as? Data, let pin = String(data: data, encoding: .utf8) {
                        cont.resume(returning: pin)
                    } else {
                        cont.resume(throwing: KeychainError.noStoredPIN)
                    }
                case errSecItemNotFound:
                    cont.resume(throwing: KeychainError.noStoredPIN)
                case errSecAuthFailed, errSecUserCanceled:
                    cont.resume(throwing: KeychainError.authenticationFailed)
                default:
                    cont.resume(throwing: KeychainError.unexpectedStatus(status))
                }
            }
        }
    }

    // MARK: - Delete

    static func clear() {
        SecItemDelete([
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ] as CFDictionary)
    }
}
