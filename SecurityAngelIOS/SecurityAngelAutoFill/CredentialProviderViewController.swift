import AuthenticationServices
import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore

/// Entry point for the AutoFill Credential Provider extension. iOS instantiates
/// this on demand whenever the user picks Security Angel from the QuickType
/// bar above the keyboard. We bootstrap Firebase (sharing the auth session
/// with the main app via the shared keychain access group), then hand off
/// to a SwiftUI hierarchy for the actual UI.
final class CredentialProviderViewController: ASCredentialProviderViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemGroupedBackground
        configureFirebaseIfNeeded()
    }

    private func configureFirebaseIfNeeded() {
        if FirebaseApp.app() == nil,
           Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        if let group = SharedKeychain.fullGroupID {
            try? Auth.auth().useUserAccessGroup(group)
        }
    }

    /// Called when the user taps Security Angel in the QuickType bar with
    /// the focused field's service identifiers (URL or app bundle id).
    override func prepareCredentialList(for serviceIdentifiers: [ASCredentialServiceIdentifier]) {
        let domain = serviceIdentifiers.first?.identifier ?? ""
        embed(.fill(domain: domain))
    }

    /// Called by iOS when the user has selected a specific credential row
    /// from the QuickType bar (no further UI required). We can short-circuit
    /// straight to the unlock step.
    override func provideCredentialWithoutUserInteraction(for credentialIdentity: ASPasswordCredentialIdentity) {
        embed(.fill(domain: credentialIdentity.serviceIdentifier.identifier))
    }

    /// Called when the user opens Security Angel via Settings →
    /// Passwords → AutoFill → Security Angel.
    override func prepareInterfaceForExtensionConfiguration() {
        embed(.config)
    }

    /// Optional: the user requested credentials but iOS needs us to show our
    /// own UI first (vault locked, etc). Identical to `prepareCredentialList`
    /// in our case.
    override func prepareInterfaceToProvideCredential(for credentialIdentity: ASPasswordCredentialIdentity) {
        embed(.fill(domain: credentialIdentity.serviceIdentifier.identifier))
    }

    // MARK: - Hosting

    enum Mode {
        case fill(domain: String)
        case config
    }

    private func embed(_ mode: Mode) {
        children.forEach {
            $0.willMove(toParent: nil)
            $0.view.removeFromSuperview()
            $0.removeFromParent()
        }

        let root = AutoFillRootView(
            mode: mode,
            onFill: { [weak self] credential in
                self?.extensionContext.completeRequest(withSelectedCredential: credential, completionHandler: nil)
            },
            onCancel: { [weak self] in
                self?.extensionContext.cancelRequest(withError: NSError(
                    domain: ASExtensionErrorDomain,
                    code: ASExtensionError.userCanceled.rawValue
                ))
            },
            onClose: { [weak self] in
                self?.extensionContext.completeExtensionConfigurationRequest()
            }
        )

        let host = UIHostingController(rootView: root)
        addChild(host)
        host.view.frame = view.bounds
        host.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(host.view)
        host.didMove(toParent: self)
    }
}

// MARK: - SwiftUI root

private struct AutoFillRootView: View {
    let mode: CredentialProviderViewController.Mode
    let onFill: (ASPasswordCredential) -> Void
    let onCancel: () -> Void
    let onClose: () -> Void

    @State private var phase: Phase = .loading
    @State private var pin: String = ""
    @State private var error: String?

    private let primary = Color(red: 0x15/255, green: 0xB7/255, blue: 0x9F/255)

    private enum Phase {
        case loading
        case notSignedIn
        case pinPrompt(salt: String, entries: [VaultEntry], domain: String)
        case credentialList(matches: [DecryptedEntry], domain: String)
        case configReady
    }

    private struct DecryptedEntry: Identifiable {
        let id: String
        let siteName: String
        let email: String
        let password: String
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemGroupedBackground).ignoresSafeArea()
                content.padding()
            }
            .navigationTitle("Security Angel")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { isConfig ? onClose() : onCancel() }
                }
            }
        }
        .task { await load() }
    }

    private var isConfig: Bool {
        if case .config = mode { return true } else { return false }
    }

    // MARK: - Phase views

    @ViewBuilder
    private var content: some View {
        switch phase {
        case .loading:
            ProgressView("Loading…").controlSize(.large)
        case .notSignedIn:
            notSignedIn
        case .pinPrompt(let salt, let entries, let domain):
            pinPrompt(salt: salt, entries: entries, domain: domain)
        case .credentialList(let matches, let domain):
            credentialList(matches: matches, domain: domain)
        case .configReady:
            configReady
        }
    }

    private var notSignedIn: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.crop.circle.badge.exclamationmark.fill")
                .font(.system(size: 64))
                .foregroundStyle(primary)
            Text("Not signed in").font(.title2.bold())
            Text("Open Security Angel and sign in to your account, then return here.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
            Button("Close") { onCancel() }
                .buttonStyle(.borderedProminent)
                .tint(primary)
        }
    }

    private func pinPrompt(salt: String, entries: [VaultEntry], domain: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 56))
                .foregroundStyle(primary)
            Text("Unlock your vault").font(.title2.bold())
            if !domain.isEmpty {
                Text("Looking for credentials for **\(domain)**")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            SecureField("Master PIN", text: $pin)
                .keyboardType(.numberPad)
                .textContentType(.password)
                .padding()
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal)
            if let error {
                Text(error).font(.caption).foregroundStyle(.red)
            }
            Button {
                tryUnlock(salt: salt, entries: entries, domain: domain)
            } label: {
                Text("Unlock").bold().frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(primary)
            .padding(.horizontal)
            .disabled(pin.isEmpty)
            Spacer()
        }
    }

    private func credentialList(matches: [DecryptedEntry], domain: String) -> some View {
        VStack(spacing: 8) {
            if matches.isEmpty {
                noMatches(domain: domain)
            } else {
                HStack {
                    Text("Choose a credential")
                        .font(.headline)
                    Spacer()
                }
                .padding(.horizontal, 4)

                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(matches) { m in
                            Button {
                                onFill(ASPasswordCredential(user: m.email, password: m.password))
                            } label: {
                                row(m)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }

    private func row(_ m: DecryptedEntry) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "globe")
                .foregroundStyle(primary)
                .frame(width: 36, height: 36)
                .background(.regularMaterial, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(m.siteName).font(.subheadline.weight(.semibold))
                Text(m.email.isEmpty ? "—" : m.email)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .padding(12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
    }

    private func noMatches(domain: String) -> some View {
        VStack(spacing: 14) {
            Image(systemName: "questionmark.app.dashed")
                .font(.system(size: 56))
                .foregroundStyle(.secondary)
            Text("No saved credentials").font(.title3.bold())
            Text("Nothing in your Security Angel vault matches **\(domain)** yet. Add it in the app, then come back here.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
            Button("Close") { onCancel() }
                .buttonStyle(.bordered)
                .tint(primary)
        }
    }

    private var configReady: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.shield.fill")
                .font(.system(size: 64))
                .foregroundStyle(primary)
            Text("All set").font(.title2.bold())
            Text("Security Angel is now your AutoFill provider. Saved logins will appear above the keyboard when you focus a password field.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
            Button("Done") { onClose() }
                .buttonStyle(.borderedProminent)
                .tint(primary)
        }
    }

    // MARK: - Pipeline

    @MainActor
    private func load() async {
        if case .config = mode {
            phase = .configReady
            return
        }
        guard case .fill(let domain) = mode else { return }

        guard let uid = Auth.auth().currentUser?.uid else {
            phase = .notSignedIn
            return
        }

        do {
            let userDoc = try await Firestore.firestore()
                .collection("users").document(uid).getDocument()
            guard let salt = userDoc.get("vaultSalt") as? String, !salt.isEmpty else {
                phase = .notSignedIn
                return
            }
            let snapshot = try await Firestore.firestore()
                .collection("users").document(uid).collection("vault").getDocuments()
            let entries = snapshot.documents.compactMap(Self.decodeVaultEntry(_:))

            if let cachedPin = try? await readPinFromKeychain() {
                pin = cachedPin
                tryUnlock(salt: salt, entries: entries, domain: domain)
            } else {
                phase = .pinPrompt(salt: salt, entries: entries, domain: domain)
            }
        } catch {
            self.error = error.localizedDescription
            phase = .notSignedIn
        }
    }

    private func tryUnlock(salt: String, entries: [VaultEntry], domain: String) {
        error = nil
        var decrypted: [DecryptedEntry] = []
        for entry in entries {
            guard DomainMatcher.match(current: domain, stored: entry.domain) else { continue }
            let email = VaultCryptoManager.decrypt(base64Ciphertext: entry.email, pin: pin, saltBase64: salt)
            let password = VaultCryptoManager.decrypt(base64Ciphertext: entry.password, pin: pin, saltBase64: salt)
            guard !password.isEmpty else { continue }
            decrypted.append(DecryptedEntry(id: entry.id, siteName: entry.siteName, email: email, password: password))
        }

        if decrypted.isEmpty && !entries.contains(where: { DomainMatcher.match(current: domain, stored: $0.domain) }) {
            phase = .credentialList(matches: [], domain: domain)
        } else if decrypted.isEmpty {
            error = "Couldn't decrypt with that PIN. Try again."
        } else {
            phase = .credentialList(matches: decrypted, domain: domain)
        }
    }

    private func readPinFromKeychain() async throws -> String {
        try await KeychainHelper.readPIN(promptReason: "Unlock to fill credentials")
    }

    // MARK: - Decode

    private static func decodeVaultEntry(_ doc: QueryDocumentSnapshot) -> VaultEntry? {
        let d = doc.data()
        let entry = VaultEntry(
            id:        doc.documentID,
            searchKey: d["searchKey"] as? String ?? "",
            siteName:  d["siteName"]  as? String ?? "",
            email:     d["email"]     as? String ?? "",
            domain:    d["domain"]    as? String ?? "",
            password:  d["password"]  as? String ?? "",
            isLeaked:  d["isLeaked"]  as? Bool   ?? false
        )
        guard !entry.password.isEmpty else { return nil }
        return entry
    }
}
