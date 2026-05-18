import Foundation
import LocalAuthentication
import UIKit

/// The iOS analogue of Android's permission monitor + device-posture sync.
/// iOS cannot enumerate third-party apps' permissions, so this aggregates
/// the device-level signals that matter for personal security.
struct DevicePosture: Hashable {
    let jailbreak: JailbreakDetector.Result
    let passcodeSet: Bool
    let biometricsEnrolled: Bool
    let biometricKind: BiometricKind
    let isProtectedDataAvailable: Bool      // proxy for "device is unlocked"
    let isOnLockdownMode: Bool
    let lastEvaluated: Date

    var isHealthy: Bool {
        !jailbreak.isJailbroken && passcodeSet && biometricsEnrolled
    }
}

enum BiometricKind: String {
    case none, touchID, faceID, opticID
}

enum DevicePostureService {

    static func evaluate() -> DevicePosture {
        let jb = JailbreakDetector.evaluate()
        let context = LAContext()
        var error: NSError?

        let bioOK = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        let kind: BiometricKind = {
            guard bioOK else { return .none }
            switch context.biometryType {
            case .touchID:  return .touchID
            case .faceID:   return .faceID
            case .opticID:  return .opticID
            @unknown default: return .none
            }
        }()

        let passcodeError: NSError? = nil
        var passcodeProbe: NSError?
        let passcodeSet = context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &passcodeProbe)
        _ = passcodeError

        let protectedAvailable = UIApplication.shared.isProtectedDataAvailable

        let lockdown: Bool = {
            if #available(iOS 17.0, *) {
                return ProcessInfo.processInfo.isiOSAppOnMac == false
                    && ProcessInfo.processInfo.environment["LOCKDOWN_MODE"] == "1"
            }
            return false
        }()

        return DevicePosture(
            jailbreak: jb,
            passcodeSet: passcodeSet,
            biometricsEnrolled: bioOK,
            biometricKind: kind,
            isProtectedDataAvailable: protectedAvailable,
            isOnLockdownMode: lockdown,
            lastEvaluated: Date()
        )
    }
}
