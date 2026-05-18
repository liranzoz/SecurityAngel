import Foundation
import FirebaseCore

/// One-shot Firebase bootstrap.
///
/// `FirebaseApp.configure()` reads `GoogleService-Info.plist` from the app
/// bundle. If the file is missing (e.g. fresh clone before the developer
/// has added it from the Firebase console), this returns `false` and the
/// rest of the app stays in an offline / setup-needed state instead of
/// crashing at startup.
enum FirebaseSupport {

    /// `true` once `FirebaseApp.configure()` has been called and the SDK
    /// has a default app. Evaluated lazily — first access triggers config.
    static let isConfigured: Bool = {
        if FirebaseApp.app() != nil { return true }
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            #if DEBUG
            print("⚠️ GoogleService-Info.plist not found in main bundle. Firebase features disabled.")
            #endif
            return false
        }
        FirebaseApp.configure()
        return FirebaseApp.app() != nil
    }()
}
