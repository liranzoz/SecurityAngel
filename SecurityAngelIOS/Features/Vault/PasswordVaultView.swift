import SwiftUI

struct PasswordVaultView: View {
    @Binding var showMenu: Bool
    @State private var accounts: [MockPasswordAccount] = MockData.passwords
    @State private var query: String = ""
    @State private var showAddSheet = false
    @State private var revealedIDs: Set<String> = []
    @State private var isLocked: Bool = true
    @State private var lockError: String? = nil

    private var filtered: [MockPasswordAccount] {
        guard !query.isEmpty else { return accounts }
        return accounts.filter { $0.siteName.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()

            if isLocked {
                lockedView
            } else {
                unlockedView
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showAddSheet) {
            AddPasswordSheet { newAccount in
                accounts.append(newAccount)
            }
            .presentationDetents([.medium, .large])
            .presentationBackground(.regularMaterial)
        }
    }

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
            PrimaryButton(title: biometricButtonTitle, icon: biometricIcon) {
                unlockWithBiometric()
            }
            .padding(.horizontal, 32)
            .disabled(!KeychainHelper.biometricsAvailable)
            .opacity(KeychainHelper.biometricsAvailable ? 1 : 0.4)

            Button("Use PIN instead") {
                withAnimation(.spring) { isLocked = false }
            }
            .foregroundStyle(Brand.primary)
            Spacer()
        }
    }

    private var biometricButtonTitle: String {
        KeychainHelper.biometricsAvailable
            ? (KeychainHelper.hasStoredPIN ? "Unlock with Face ID" : "Face ID not configured")
            : "Biometrics unavailable"
    }

    private var biometricIcon: String {
        DevicePostureService.evaluate().biometricKind == .touchID ? "touchid" : "faceid"
    }

    private func unlockWithBiometric() {
        lockError = nil
        guard KeychainHelper.hasStoredPIN else {
            lockError = "No PIN stored yet — set one up after sign-in."
            withAnimation(.spring) { isLocked = false }
            return
        }
        Task {
            do {
                _ = try await KeychainHelper.readPIN()
                await MainActor.run {
                    withAnimation(.spring) { isLocked = false }
                }
            } catch {
                await MainActor.run { lockError = "Authentication failed. Try the PIN." }
            }
        }
    }

    private var unlockedView: some View {
        VStack(spacing: 12) {
            ScreenTitleBar(
                title: "Password Vault",
                onMenu: { showMenu = true },
                trailing: AnyView(
                    Button {} label: {
                        Image(systemName: "arrow.triangle.2.circlepath")
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(.primary)
                            .frame(width: 40, height: 40)
                            .liquidGlassCard(cornerRadius: 12)
                    }
                )
            )
            .padding(.top, 8)

            GlassSearchField(placeholder: "Search accounts…", text: $query)
                .padding(.horizontal)

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(filtered) { account in
                        PasswordRow(
                            account: account,
                            isRevealed: revealedIDs.contains(account.id),
                            onToggleReveal: { toggleReveal(account.id) },
                            onCopy: { copyPassword(account.password) }
                        )
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
            .padding(.bottom, 90)
        }
    }

    private func toggleReveal(_ id: String) {
        if revealedIDs.contains(id) {
            revealedIDs.remove(id)
        } else {
            revealedIDs.insert(id)
        }
    }

    private func copyPassword(_ password: String) {
        UIPasteboard.general.string = password
    }
}

#Preview {
    PasswordVaultView(showMenu: .constant(false))
}
