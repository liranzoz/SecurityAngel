import SwiftUI

struct PasswordVaultView: View {
    @Binding var showMenu: Bool
    @Environment(AppState.self) private var appState

    @State private var query: String = ""
    @State private var showAddSheet = false
    @State private var editing: VaultEntry? = nil
    @State private var revealedIDs: Set<String> = []
    @State private var lockError: String? = nil
    @State private var isUnlocking: Bool = false
    @State private var scanningLeaks: Bool = false
    @State private var leakResultMessage: String? = nil
    @State private var showPinPrompt = false
    @State private var pinInput = ""
    @State private var shouldStoreInKeychain = false

    private var entries: [VaultEntry] { appState.vaultEntries }
    private var filteredEntries: [VaultEntry] {
        guard !query.isEmpty else { return entries }
        return entries.filter { $0.siteName.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            if !appState.vaultSession.isValid {
                lockedView
            } else {
                unlockedView
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showAddSheet) {
            AddPasswordSheet(editing: nil)
                .environment(appState)
                .presentationDetents([.medium, .large])
                .presentationBackground(.regularMaterial)
        }
        .sheet(item: $editing) { entry in
            AddPasswordSheet(editing: entry)
                .environment(appState)
                .presentationDetents([.medium, .large])
                .presentationBackground(.regularMaterial)
        }
        .alert("Enter Vault PIN", isPresented: $showPinPrompt) {
            SecureField("PIN", text: $pinInput)
                .keyboardType(.numberPad)
            Toggle("Save to Face ID", isOn: $shouldStoreInKeychain)
            Button("Unlock") { Task { await applyPin() } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Use the same master PIN as the Android app to decrypt your vault.")
        }
    }

    // MARK: - Locked

    private var lockedView: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 80))
                .foregroundStyle(Brand.primary)
                .padding(40)
                .liquidGlass(in: Circle())
            Text("Vault Locked")
                .font(Typography.display)
            Text("Authenticate with Face ID or your master PIN to view your saved passwords.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 40)
            if let lockError {
                Text(lockError)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(.horizontal, 32)
            }
            if KeychainHelper.biometricsAvailable && KeychainHelper.hasStoredPIN {
                PrimaryButton(title: "Unlock with Face ID", icon: "faceid", isLoading: isUnlocking) {
                    unlockWithBiometric()
                }
                .padding(.horizontal, 32)
            }
            Button("Use PIN") { promptPin() }
                .foregroundStyle(Brand.primary)
            Spacer()
        }
    }

    private func promptPin() {
        pinInput = ""
        showPinPrompt = true
    }

    // MARK: - Unlocked

    private var unlockedView: some View {
        VStack(spacing: 12) {
            ScreenTitleBar(
                title: "Password Vault",
                onMenu: { showMenu = true },
                trailing: AnyView(
                    Button { scanForLeaks() } label: {
                        Image(systemName: scanningLeaks ? "arrow.triangle.2.circlepath.circle.fill" : "arrow.triangle.2.circlepath")
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(.primary)
                            .frame(width: 40, height: 40)
                            .liquidGlassCard(cornerRadius: 12)
                    }
                    .disabled(scanningLeaks)
                )
            )
            .padding(.top, 8)

            GlassSearchField(placeholder: "Search accounts…", text: $query)
                .padding(.horizontal)

            if let leakResultMessage {
                Text(leakResultMessage)
                    .font(.caption.bold())
                    .padding(.horizontal, 16).padding(.vertical, 8)
                    .liquidGlassCapsule()
                    .padding(.horizontal)
                    .transition(.opacity)
            }

            ScrollView {
                LazyVStack(spacing: 10) {
                    if filteredEntries.isEmpty {
                        emptyState
                    } else {
                        ForEach(filteredEntries) { entry in
                            VaultEntryRow(
                                entry: entry,
                                pin: appState.vaultSession.masterPin,
                                salt: appState.vaultSession.vaultSalt,
                                isRevealed: revealedIDs.contains(entry.id),
                                onToggleReveal: { toggle(entry) },
                                onEdit: { editing = entry },
                                onDelete: { delete(entry) }
                            )
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 100)
            }
        }
        .overlay(alignment: .bottomTrailing) {
            Button { showAddSheet = true } label: {
                Image(systemName: "plus")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(Brand.headerGradient, in: Circle())
                    .shadow(color: Brand.primary.opacity(0.45), radius: 14, y: 8)
            }
            .padding(.trailing, 24)
            .padding(.bottom, 24)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "key.viewfinder")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
                .padding(.top, 40)
            Text("Your vault is empty").font(Typography.title)
            Text("Tap + to add your first password.")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 40)
    }

    // MARK: - Actions

    private func unlockWithBiometric() {
        lockError = nil
        isUnlocking = true
        Task {
            do {
                let pin = try await KeychainHelper.readPIN()
                let salt = try await ensureSalt()
                appState.vaultSession.save(pin: pin, salt: salt)
                isUnlocking = false
            } catch {
                lockError = "Authentication failed. Use PIN instead."
                isUnlocking = false
            }
        }
    }

    private func applyPin() async {
        guard !pinInput.isEmpty else { return }
        do {
            let salt = try await ensureSalt()
            appState.vaultSession.save(pin: pinInput, salt: salt)
            if shouldStoreInKeychain {
                try? KeychainHelper.storePIN(pinInput)
            }
            pinInput = ""
        } catch {
            lockError = error.localizedDescription
        }
    }

    private func ensureSalt() async throws -> String {
        guard let uid = appState.firebaseUserId else { throw AuthRepoError.notSignedIn }
        if let salt = appState.currentUser?.vaultSalt, !salt.isEmpty { return salt }
        return try await appState.userRepo.ensureVaultSalt(uid: uid)
    }

    private func toggle(_ entry: VaultEntry) {
        if revealedIDs.contains(entry.id) {
            revealedIDs.remove(entry.id)
        } else {
            revealedIDs.insert(entry.id)
        }
    }

    private func delete(_ entry: VaultEntry) {
        guard let uid = appState.firebaseUserId else { return }
        Task { try? await appState.vaultRepo.delete(uid: uid, entryId: entry.id) }
    }

    private func scanForLeaks() {
        guard let uid = appState.firebaseUserId, !entries.isEmpty else { return }
        scanningLeaks = true
        leakResultMessage = nil
        Task {
            let pin  = appState.vaultSession.masterPin
            let salt = appState.vaultSession.vaultSalt
            var found = 0
            for entry in entries {
                let decrypted = VaultCryptoManager.decrypt(base64Ciphertext: entry.password, pin: pin, saltBase64: salt)
                guard !decrypted.isEmpty else { continue }
                let leaked = await HaveIBeenPwnedAPI.isLeaked(password: decrypted)
                if leaked != entry.isLeaked {
                    try? await appState.vaultRepo.markLeaked(uid: uid, entryId: entry.id, isLeaked: leaked)
                }
                if leaked { found += 1 }
            }
            if found > 0 {
                try? await appState.logger.reportRisk(uid: uid, risk: .passwordLeak)
                try? await appState.logger.logEvent(
                    uid: uid, eventType: .leakFound,
                    description: "Found \(found) leaked password\(found == 1 ? "" : "s") in vault."
                )
                leakResultMessage = "⚠️ \(found) compromised password\(found == 1 ? "" : "s") found"
            } else {
                try? await appState.logger.resolveRisk(uid: uid, risk: .passwordLeak)
                leakResultMessage = "✅ No leaks found"
            }
            scanningLeaks = false
        }
    }
}

#Preview {
    PasswordVaultView(showMenu: .constant(false))
        .environment(AppState())
}
