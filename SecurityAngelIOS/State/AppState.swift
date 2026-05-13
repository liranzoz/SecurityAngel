import Foundation
import Observation
import FirebaseAuth
import FirebaseFirestore

/// Single source of truth for app-level state. Owns one instance of each
/// repository and tracks the currently signed-in user, their Firestore
/// document, family membership, and vault entries. Views observe this via
/// the SwiftUI environment.
@MainActor
@Observable
final class AppState {

    // Repositories
    let authRepo       = AuthRepository()
    let userRepo       = UserRepository()
    let familyRepo     = FamilyRepository()
    let vaultRepo      = VaultRepository()
    let scanRepo       = ScanHistoryRepository()
    let chatRepo       = ChatHistoryRepository()
    let logger         = SecurityLogger()
    let invitationRepo = InvitationRepository()

    // Session
    let vaultSession   = VaultSession()

    // Realtime state
    var firebaseUserId: String?   = nil
    var currentUser:    SecurityUser?    = nil
    var family:         SecurityFamily?  = nil
    var familyMembers:  [SecurityUser]   = []
    var vaultEntries:   [VaultEntry]     = []
    var recentScans:    [ScanHistoryItem] = []

    // UI flags
    var devicePosture: DevicePosture = DevicePostureService.evaluate()
    var firebaseAvailable: Bool { FirebaseSupport.isConfigured }

    // Internal handles
    private var authHandle: AuthStateDidChangeListenerHandle?
    private var userListener:    ListenerRegistration?
    private var membersListener: ListenerRegistration?
    private var vaultListener:   ListenerRegistration?
    private var scanListener:    ListenerRegistration?
    private var currentFamilyId: String? = nil

    // MARK: - Lifecycle

    init() {
        guard FirebaseSupport.isConfigured else { return }
        authHandle = authRepo.addStateListener { [weak self] user in
            Task { @MainActor [weak self] in self?.handleAuthChange(user) }
        }
    }

    private func handleAuthChange(_ user: User?) {
        if let user, user.isEmailVerified {
            firebaseUserId = user.uid
            startListening(uid: user.uid)
        } else {
            firebaseUserId = nil
            currentUser = nil
            family = nil
            familyMembers = []
            vaultEntries = []
            recentScans = []
            currentFamilyId = nil
            userListener?.remove();    userListener = nil
            membersListener?.remove(); membersListener = nil
            vaultListener?.remove();   vaultListener = nil
            scanListener?.remove();    scanListener = nil
            vaultSession.clear()
        }
    }

    private func startListening(uid: String) {
        userListener?.remove()
        userListener = userRepo.observe(id: uid) { [weak self] user in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.currentUser = user
                if let familyId = user?.familyId, !familyId.isEmpty {
                    if familyId != self.currentFamilyId {
                        self.attachFamily(familyId: familyId)
                    }
                } else {
                    self.family = nil
                    self.familyMembers = []
                    self.currentFamilyId = nil
                    self.membersListener?.remove()
                }
            }
        }

        vaultListener?.remove()
        vaultListener = vaultRepo.observe(uid: uid) { [weak self] entries in
            Task { @MainActor [weak self] in self?.vaultEntries = entries }
        }

        scanListener?.remove()
        scanListener = scanRepo.observeRecent(uid: uid) { [weak self] items in
            Task { @MainActor [weak self] in self?.recentScans = items }
        }
    }

    private func attachFamily(familyId: String) {
        currentFamilyId = familyId
        Task {
            self.family = try? await familyRepo.get(id: familyId)
            self.membersListener?.remove()
            self.membersListener = self.familyRepo.observeMembers(familyId: familyId) { [weak self] members in
                Task { @MainActor [weak self] in self?.familyMembers = members }
            }
        }
    }

    // MARK: - Auth actions

    func signOut() {
        try? authRepo.signOut()
    }

    // MARK: - Derived

    var isAuthenticated: Bool { firebaseUserId != nil }

    var isAdmin: Bool {
        guard let family, let uid = currentUser?.id else { return false }
        return family.adminId == uid
    }

    var leakedPasswordCount: Int {
        vaultEntries.filter(\.isLeaked).count
    }

    var totalPasswordCount: Int {
        vaultEntries.count
    }

    var personalScore: Int {
        ScoreCalculator.calculatePersonalScore(leakedPasswordCount: leakedPasswordCount)
    }

    /// Final score shown on the Dashboard. Admin gets a family-blended
    /// score; everyone else just sees their personal score. Device-posture
    /// (jailbreak / no biometrics) blends in as a permissions sub-score.
    var dashboardScore: ScoreCalculator.Result {
        let baseScore: Int = {
            guard isAdmin else { return personalScore }
            let others = familyMembers
                .filter { $0.id != currentUser?.id }
                .map(\.securityScore)
            return ScoreCalculator.blendFamilyScore(myScore: personalScore, memberScores: others)
        }()
        let high = devicePosture.jailbreak.isJailbroken ? 1 : 0
        let medium = (!devicePosture.passcodeSet || !devicePosture.biometricsEnrolled) ? 1 : 0
        return ScoreCalculator.integratePermissionsScore(
            currentGlobalScore: baseScore,
            highRiskCount: high,
            mediumRiskCount: medium,
            lowRiskCount: 0
        )
    }

    var familyAlertCount: Int {
        familyMembers.reduce(0) { $0 + $1.riskCount }
    }
}
