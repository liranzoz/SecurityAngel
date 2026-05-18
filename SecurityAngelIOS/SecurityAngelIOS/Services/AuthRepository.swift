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
        do {
            let result = try await auth.signIn(withEmail: email, password: password)
            guard result.user.isEmailVerified else {
                try? auth.signOut()
                throw AuthRepoError.notVerified
            }
            return result.user
        } catch let error as NSError where error.domain == AuthErrorDomain {
            throw mapAuthError(error)
        }
    }

    /// Creates a new Firebase user and sends them a verification email.
    /// The Firestore user document is created separately by `UserRepository`
    /// once the user has confirmed (or immediately for Google sign-in).
    func signUp(email: String, password: String) async throws -> User {
        do {
            let result = try await auth.createUser(withEmail: email, password: password)
            try await result.user.sendEmailVerification()
            return result.user
        } catch let error as NSError where error.domain == AuthErrorDomain {
            throw mapAuthError(error)
        }
    }

    /// Send the standard Firebase reset-password email for the supplied
    /// address. Doesn't reveal whether the address exists when email
    /// enumeration protection is enabled — Firebase responds the same way
    /// either way.
    func sendPasswordReset(email: String) async throws {
        do {
            try await auth.sendPasswordReset(withEmail: email)
        } catch let error as NSError where error.domain == AuthErrorDomain {
            throw mapAuthError(error)
        }
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

    // MARK: - Error mapping

    private func mapAuthError(_ error: NSError) -> AuthRepoError {
        guard let code = AuthErrorCode(rawValue: error.code) else {
            return .underlying(error)
        }
        switch code {
        case .invalidCredential, .wrongPassword, .userNotFound:
            return .invalidCredentials
        case .invalidEmail:
            return .invalidEmail
        case .emailAlreadyInUse:
            return .emailInUse
        case .weakPassword:
            return .weakPassword
        case .networkError:
            return .network
        case .tooManyRequests:
            return .tooManyRequests
        case .userDisabled:
            return .userDisabled
        default:
            return .underlying(error)
        }
    }
}

enum AuthRepoError: LocalizedError {
    case notVerified
    case notSignedIn
    case invalidCredentials
    case invalidEmail
    case emailInUse
    case weakPassword
    case network
    case tooManyRequests
    case userDisabled
    case underlying(Error)

    var errorDescription: String? {
        switch self {
        case .notVerified:
            return "Please verify your email before signing in."
        case .notSignedIn:
            return "Not signed in."
        case .invalidCredentials:
            return "Email or password is incorrect. If you've forgotten your password, tap Forgot password."
        case .invalidEmail:
            return "That email address doesn't look valid."
        case .emailInUse:
            return "This email is already registered. Try signing in instead."
        case .weakPassword:
            return "Password is too weak — use at least 6 characters."
        case .network:
            return "Network error. Check your connection and try again."
        case .tooManyRequests:
            return "Too many attempts. Wait a moment and try again."
        case .userDisabled:
            return "This account has been disabled."
        case .underlying(let error):
            return error.localizedDescription
        }
    }
}
