import SwiftUI

struct SignUpView: View {
    @Environment(AppState.self) private var appState
    @Environment(\.dismiss) private var dismiss

    @State private var firstName = ""
    @State private var lastName = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var password = ""
    @State private var gender: String = "Male"
    @State private var didSendVerification = false
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    private var isValid: Bool {
        !firstName.isEmpty && !lastName.isEmpty && !phone.isEmpty && !email.isEmpty && password.count >= 6
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.backgroundGradient.ignoresSafeArea()
                ScrollView {
                    if didSendVerification {
                        verificationContent
                    } else {
                        formContent
                    }
                }
            }
            .navigationTitle("Create Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private var formContent: some View {
        VStack(spacing: 16) {
            GlassCard(padding: 24) {
                VStack(spacing: 14) {
                    HStack(spacing: 12) {
                        GlassTextField(placeholder: "First name", text: $firstName, icon: "person.fill")
                        GlassTextField(placeholder: "Last name",  text: $lastName,  icon: "person.fill")
                    }
                    GlassTextField(placeholder: "Phone", text: $phone, icon: "phone.fill", keyboardType: .phonePad, contentType: .telephoneNumber)
                    GlassTextField(placeholder: "Email", text: $email, icon: "envelope.fill", keyboardType: .emailAddress, contentType: .emailAddress)
                    GlassTextField(placeholder: "Password (6+ chars)", text: $password, icon: "lock.fill", isSecure: true, contentType: .newPassword)

                    Picker("Gender", selection: $gender) {
                        Text("Male").tag("Male")
                        Text("Female").tag("Female")
                        Text("Other").tag("Other")
                    }
                    .pickerStyle(.segmented)

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundStyle(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }

            PrimaryButton(title: "Create Account", icon: "checkmark.circle.fill", isLoading: isSubmitting) {
                signUp()
            }
            .disabled(!isValid || isSubmitting)
            .opacity(isValid ? 1 : 0.5)
        }
        .padding()
    }

    private var verificationContent: some View {
        VStack(spacing: 24) {
            Image(systemName: "envelope.badge.shield.half.filled")
                .font(.system(size: 80))
                .foregroundStyle(Brand.primary)
                .padding(.top, 40)

            VStack(spacing: 8) {
                Text("Check your inbox")
                    .font(Typography.display)
                Text("We sent a link to\n\(email)\n\nClick it, then come back and tap below.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 32)

            PrimaryButton(title: "I've verified", icon: "checkmark.circle.fill", isLoading: isSubmitting) {
                completeAfterVerification()
            }
            .padding(.horizontal)

            Button("Resend verification email") { resendVerification() }
                .foregroundStyle(Brand.primary)

            if let errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding()
            }
        }
        .padding()
    }

    // MARK: - Actions

    private func signUp() {
        errorMessage = nil
        isSubmitting = true
        Task {
            do {
                let user = try await appState.authRepo.signUp(email: email, password: password)
                let model = SecurityUser(
                    id: user.uid,
                    firstName: firstName,
                    lastName: lastName,
                    email: email,
                    phone: phone,
                    gender: gender,
                    vaultSalt: VaultCryptoManager.generateSalt()
                )
                try await appState.userRepo.create(model)
                didSendVerification = true
            } catch {
                errorMessage = error.localizedDescription
            }
            isSubmitting = false
        }
    }

    private func completeAfterVerification() {
        errorMessage = nil
        isSubmitting = true
        Task {
            do {
                let verified = try await appState.authRepo.reloadAndCheckVerification()
                if verified {
                    // AppState's auth listener will flip the root view.
                    dismiss()
                } else {
                    errorMessage = "Email isn't verified yet — tap the link in your inbox first."
                }
            } catch {
                errorMessage = error.localizedDescription
            }
            isSubmitting = false
        }
    }

    private func resendVerification() {
        Task {
            do { try await appState.authRepo.resendVerification() }
            catch { errorMessage = error.localizedDescription }
        }
    }
}

#Preview {
    SignUpView()
        .environment(AppState())
}
