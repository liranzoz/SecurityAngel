import Foundation
import UIKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn

/// Wraps the Google Sign-In flow + Firebase credential exchange. Matches
/// the Android app's flow in `LoginActivity.setupGoogleSignIn`:
///
/// 1. Present the Google sign-in sheet from the supplied view controller.
/// 2. Exchange the resulting Google id-token for a Firebase Auth credential.
/// 3. `Auth.auth().signIn(with:)` — Firebase creates the user if new,
///    reuses the existing one if not.
///
/// Whether the Firebase user already has a Firestore `users/{uid}` doc
/// is the caller's concern (LoginView checks and presents the signup
/// completion sheet for first-time Google sign-ins, exactly like the
/// Android `checkAndCreateUserInFirestore` does).
enum GoogleSignInService {

    enum SignInError: LocalizedError {
        case missingClientID
        case missingIDToken
        case noPresenter

        var errorDescription: String? {
            switch self {
            case .missingClientID: return "Firebase isn't configured. Drop in GoogleService-Info.plist."
            case .missingIDToken:  return "Google didn't return an ID token."
            case .noPresenter:     return "Couldn't find a view controller to present from."
            }
        }
    }

    /// Wire `GIDSignIn` to the Firebase project's iOS OAuth client. Must
    /// be called once before any sign-in attempt; called from
    /// `SecurityAngelIOSApp.init()`.
    static func configure() {
        guard let clientID = FirebaseApp.app()?.options.clientID else { return }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
    }

    /// Forwards a sign-in callback URL to the Google SDK. Hooked up via
    /// `.onOpenURL` on the SwiftUI App.
    @discardableResult
    static func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    /// Sign in to Google, then exchange the token for a Firebase session.
    /// Throws on user cancellation, network failure, or token issues.
    @MainActor
    static func signIn() async throws -> (user: User, displayName: String?, email: String?, isNewUser: Bool) {
        guard let clientID = FirebaseApp.app()?.options.clientID else { throw SignInError.missingClientID }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        guard let presenter = topViewController() else { throw SignInError.noPresenter }

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        guard let idToken = result.user.idToken?.tokenString else { throw SignInError.missingIDToken }
        let accessToken = result.user.accessToken.tokenString

        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
        let authResult = try await Auth.auth().signIn(with: credential)

        return (
            user: authResult.user,
            displayName: result.user.profile?.name,
            email: result.user.profile?.email ?? authResult.user.email,
            isNewUser: authResult.additionalUserInfo?.isNewUser ?? false
        )
    }

    static func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    // MARK: - Helpers

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let windows = scenes.flatMap { $0.windows }.filter(\.isKeyWindow)
        guard let root = windows.first?.rootViewController else { return nil }
        return topController(from: root)
    }

    private static func topController(from controller: UIViewController) -> UIViewController {
        if let presented = controller.presentedViewController {
            return topController(from: presented)
        }
        if let nav = controller as? UINavigationController, let visible = nav.visibleViewController {
            return topController(from: visible)
        }
        if let tab = controller as? UITabBarController, let selected = tab.selectedViewController {
            return topController(from: selected)
        }
        return controller
    }
}
