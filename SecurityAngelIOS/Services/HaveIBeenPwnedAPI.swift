import Foundation
import CryptoKit

/// Privacy-preserving password breach check via the HIBP "Range" endpoint
/// (k-anonymity). Only the first 5 chars of the SHA-1 hash are sent over
/// the wire. The plaintext password never leaves the device.
///
/// Matches `BreachCheckUtil.kt` on Android byte-for-byte.
enum HaveIBeenPwnedAPI {

    /// Returns `true` if the password's SHA-1 suffix appears in the HIBP
    /// response, `false` otherwise. Network/parse failures return `false`
    /// (fail-open, matching Android behavior).
    static func isLeaked(password: String) async -> Bool {
        let sha1Hex = sha1Hex(password).uppercased()
        let prefix = String(sha1Hex.prefix(5))
        let suffix = String(sha1Hex.dropFirst(5))

        guard let url = URL(string: "https://api.pwnedpasswords.com/range/\(prefix)") else { return false }
        var req = URLRequest(url: url)
        req.setValue("SecurityAngeliOS", forHTTPHeaderField: "User-Agent")

        do {
            let (data, response) = try await URLSession.shared.data(for: req)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
                  let body = String(data: data, encoding: .utf8) else { return false }
            return body.uppercased().contains(suffix)
        } catch {
            return false
        }
    }

    private static func sha1Hex(_ input: String) -> String {
        let digest = Insecure.SHA1.hash(data: Data(input.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
