import Foundation
import FirebaseAuth

/// Wraps Firebase Auth. Email + password only for now; Google Sign-In is a
/// later phase (needs URL-scheme config in Info.plist).
final class AuthRepository {

    private var auth: Auth { Auth.auth() }

    var currentUserId: String? { auth.currentUser?.uid }
    var isEmailVerified: Bool { auth.currentUser?.isEmailVerified ?? false }
    var currentUserEmail: String? { auth.currentUser?.email }

    // MARK: - Sign in / up

    /// Signs in. Throws `AuthRepoError.notVerified` if the email is not
    /// verified (matches Android behavior — Android also signs the user
    /// straight back out in that case).
    func signIn(email: String, password: String) async throws -> User {
        let result = try await auth.signIn(withEmail: email, password: password)
        guard result.user.isEmailVerified else {
            try? auth.signOut()
            throw AuthRepoError.notVerified
        }
        return result.user
    }

    /// Creates a new Firebase user and sends them a verification email.
    /// The Firestore user document is created separately by `UserRepository`
    /// once the user has confirmed (or immediately for Google sign-in).
    func signUp(email: String, password: String) async throws -> User {
        let result = try await auth.createUser(withEmail: email, password: password)
        try await result.user.sendEmailVerification()
        return result.user
    }

    /// Reload and return whether the email is verified.
    func reloadAndCheckVerification() async throws -> Bool {
        guard let user = auth.currentUser else { throw AuthRepoError.notSignedIn }
        try await user.reload()
        return user.isEmailVerified
    }

    func resendVerification() async throws {
        guard let user = auth.currentUser else { throw AuthRepoError.notSignedIn }
        try await user.sendEmailVerification()
    }

    func signOut() throws {
        try auth.signOut()
    }

    // MARK: - Listener

    func addStateListener(_ onChange: @escaping (User?) -> Void) -> AuthStateDidChangeListenerHandle {
        auth.addStateDidChangeListener { _, user in onChange(user) }
    }

    func removeStateListener(_ handle: AuthStateDidChangeListenerHandle) {
        auth.removeStateDidChangeListener(handle)
    }
}

enum AuthRepoError: LocalizedError {
    case notVerified
    case notSignedIn

    var errorDescription: String? {
        switch self {
        case .notVerified:  return "Please verify your email before signing in."
        case .notSignedIn:  return "Not signed in."
        }
    }
}
