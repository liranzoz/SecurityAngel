import SwiftUI

struct AddPasswordSheet: View {
    let editing: VaultEntry?

    @Environment(AppState.self) private var appState
    @Environment(\.dismiss) private var dismiss

    @State private var siteName = ""
    @State private var domain = ""
    @State private var email = ""
    @State private var password = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    private var isValid: Bool { !siteName.isEmpty && !password.isEmpty }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    GlassTextField(placeholder: "Site name (e.g. GitHub)", text: $siteName, icon: "globe")
                    GlassTextField(placeholder: "Domain (e.g. github.com)", text: $domain, icon: "link")
                    GlassTextField(placeholder: "Email or username", text: $email, icon: "person.fill", contentType: .username)
                    GlassTextField(placeholder: "Password", text: $password, icon: "lock.fill", isSecure: true, contentType: .newPassword)
                    if let errorMessage {
                        Text(errorMessage).font(.caption).foregroundStyle(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding()
            }
            .background(Brand.backgroundGradient.ignoresSafeArea())
            .navigationTitle(editing == nil ? "Add Password" : "Edit Password")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(!isValid || isSaving)
                }
            }
            .onAppear { prefill() }
        }
    }

    private func prefill() {
        guard let editing else { return }
        siteName = editing.siteName
        domain   = editing.domain
        let pin  = appState.vaultSession.masterPin
        let salt = appState.vaultSession.vaultSalt
        email    = VaultCryptoManager.decrypt(base64Ciphertext: editing.email,    pin: pin, saltBase64: salt)
        password = VaultCryptoManager.decrypt(base64Ciphertext: editing.password, pin: pin, saltBase64: salt)
    }

    private func save() {
        guard let uid = appState.firebaseUserId else { return }
        let pin  = appState.vaultSession.masterPin
        let salt = appState.vaultSession.vaultSalt
        isSaving = true
        Task {
            do {
                if let editing {
                    try await appState.vaultRepo.update(
                        uid: uid, entryId: editing.id,
                        siteName: siteName, domain: domain,
                        email: email, password: password,
                        pin: pin, salt: salt
                    )
                } else {
                    try await appState.vaultRepo.add(
                        uid: uid,
                        siteName: siteName, domain: domain,
                        email: email, password: password,
                        pin: pin, salt: salt
                    )
                }
                dismiss()
            } catch {
                errorMessage = error.localizedDescription
            }
            isSaving = false
        }
    }
}

#Preview {
    AddPasswordSheet(editing: nil)
        .environment(AppState())
}
