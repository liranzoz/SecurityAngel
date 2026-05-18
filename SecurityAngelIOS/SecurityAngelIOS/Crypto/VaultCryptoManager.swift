import Foundation
import CryptoKit
import CommonCrypto

/// Byte-for-byte compatible with the Android `VaultCryptoManager.kt`:
///
///   key   = PBKDF2-HMAC-SHA256(pin, base64decode(salt), 100_000 iters, 256-bit)
///   blob  = AES/GCM/NoPadding (12-byte IV, 128-bit tag)
///   wire  = base64( iv ‖ ciphertext ‖ tag )      — `Base64.NO_WRAP`
///
/// CryptoKit's `AES.GCM.SealedBox.combined` returns `nonce ‖ ciphertext ‖ tag`
/// with a 12-byte nonce and 16-byte (128-bit) tag, which matches Android's
/// layout exactly. Vault docs encrypted on Android decrypt on iOS and vice
/// versa with no transform.
enum VaultCryptoManager {

    private static let iterations: Int   = 100_000
    private static let keyByteCount: Int = 32   // 256 bits
    private static let saltByteCount: Int = 16

    enum CryptoError: Error {
        case pbkdf2Failed
        case invalidSalt
        case invalidCiphertext
        case decryptionFailed
    }

    // MARK: - Salt

    static func generateSalt() -> String {
        var bytes = [UInt8](repeating: 0, count: saltByteCount)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return Data(bytes).base64EncodedString(options: [])
    }

    // MARK: - Key derivation

    static func deriveKey(pin: String, saltBase64: String) throws -> SymmetricKey {
        guard let salt = Data(base64Encoded: saltBase64) else { throw CryptoError.invalidSalt }

        var derived = [UInt8](repeating: 0, count: keyByteCount)
        let result = pin.withCString { pinPtr in
            salt.withUnsafeBytes { saltBytes -> Int32 in
                CCKeyDerivationPBKDF(
                    CCPBKDFAlgorithm(kCCPBKDF2),
                    pinPtr, strlen(pinPtr),
                    saltBytes.bindMemory(to: UInt8.self).baseAddress, salt.count,
                    CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                    UInt32(iterations),
                    &derived, derived.count
                )
            }
        }
        guard result == kCCSuccess else { throw CryptoError.pbkdf2Failed }
        return SymmetricKey(data: Data(derived))
    }

    // MARK: - Encrypt / decrypt

    static func encrypt(plaintext: String, pin: String, saltBase64: String) throws -> String {
        let key = try deriveKey(pin: pin, saltBase64: saltBase64)
        let sealed = try AES.GCM.seal(Data(plaintext.utf8), using: key)
        guard let combined = sealed.combined else { throw CryptoError.invalidCiphertext }
        return combined.base64EncodedString(options: [])
    }

    /// Returns plaintext on success, empty string on failure — matching the
    /// Android `decrypt` contract (`try { … } catch (_: Exception) { "" }`).
    /// Throws only on inputs that are obviously malformed (bad base64).
    static func decrypt(base64Ciphertext: String, pin: String, saltBase64: String) -> String {
        guard let combined = Data(base64Encoded: base64Ciphertext), combined.count > 12 + 16 else { return "" }
        do {
            let key = try deriveKey(pin: pin, saltBase64: saltBase64)
            let sealed = try AES.GCM.SealedBox(combined: combined)
            let plaintext = try AES.GCM.open(sealed, using: key)
            return String(data: plaintext, encoding: .utf8) ?? ""
        } catch {
            return ""
        }
    }
}
