import Foundation
import Observation

/// In-memory cache of the user's vault master PIN + per-user salt for the
/// duration of the app session. Equivalent to Android's `VaultSessionManager`.
///
/// The PIN is never persisted here. Long-term storage of the PIN goes
/// through `KeychainHelper` (biometric-protected). This object exists so
/// the user only has to authenticate once per app session.
@Observable
@MainActor
final class VaultSession {

    private(set) var masterPin: String = ""
    private(set) var vaultSalt: String = ""

    var isValid: Bool { !masterPin.isEmpty && !vaultSalt.isEmpty }

    func save(pin: String, salt: String) {
        masterPin = pin
        vaultSalt = salt
    }

    func clear() {
        masterPin = ""
        vaultSalt = ""
    }
}
