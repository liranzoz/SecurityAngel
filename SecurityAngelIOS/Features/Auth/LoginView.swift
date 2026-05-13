import SwiftUI
import FirebaseAuth

struct LoginView: View {
    @Environment(AppState.self) private var appState

    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var isGoogleLoading = false
    @State private var showSignUp = false
    @State private var errorMessage: String?
    @State private var resetSentForEmail: String?
    @State private var showResetPrompt = false
    @State private var resetEmail = ""
    @State private var googleSignupContext: GoogleSignupContext?

    var body: some View {
        ZStack {
            Brand.headerGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 28) {
                    Spacer(minLength: 60)

                    VStack(spacing: 12) {
                        AppLogo()
                            .padding(.bottom, 4)
                        Text("Welcome back")
                            .font(.headline)
                            .foregroundStyle(.white.opacity(0.85))
                    }

                    GlassCard(padding: 24) {
                        VStack(spacing: 16) {
                            GlassTextField(
                                placeholder: "Email",
                                text: $email,
                                icon: "envelope.fill",
                                keyboardType: .emailAddress,
                                contentType: .emailAddress
                            )
                            GlassTextField(
                                placeholder: "Password",
                                text: $password,
                                icon: "lock.fill",
                                isSecure: true,
                                contentType: .password
                            )

                            if let errorMessage {
                                Text(errorMessage)
                                    .font(.caption)
                                    .foregroundStyle(.red)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }

                            if let resetSentForEmail {
                                Label("Reset link sent to \(resetSentForEmail).", systemImage: "envelope.badge.fill")
                                    .font(.caption)
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(Brand.primary.opacity(0.6), in: Capsule())
                            }

                            PrimaryButton(title: "Sign In", icon: "arrow.right", isLoading: isLoading) {
                                signIn()
                            }
                            .disabled(email.isEmpty || password.isEmpty || isLoading)
                            .opacity((email.isEmpty || password.isEmpty) ? 0.5 : 1)

                            HStack {
                                Rectangle().fill(.white.opacity(0.25)).frame(height: 1)
                                Text("or").font(.caption).foregroundStyle(.secondary)
                                Rectangle().fill(.white.opacity(0.25)).frame(height: 1)
                            }

                            Button {
                                signInWithGoogle()
                            } label: {
                                HStack(spacing: 10) {
                                    if isGoogleLoading {
                                        ProgressView().tint(.primary)
                                    } else {
                                        Image(systemName: "g.circle.fill").foregroundStyle(.red)
                                    }
                                    Text(isGoogleLoading ? "Signing in…" : "Continue with Google").font(.headline)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .foregroundStyle(.primary)
                                .liquidGlassCapsule()
                            }
                            .disabled(isGoogleLoading)

                            Button("Forgot password?") {
                                resetEmail = email
                                showResetPrompt = true
                            }
                            .font(.footnote)
                            .foregroundStyle(Brand.primaryDark)
                        }
                    }
                    .padding(.horizontal, 20)

                    Button {
                        showSignUp = true
                    } label: {
                        HStack(spacing: 4) {
                            Text("New here?").foregroundStyle(.white.opacity(0.85))
                            Text("Create an account").bold().foregroundStyle(.white)
                        }
                        .font(.callout)
                    }

                    Spacer(minLength: 24)
                }
            }
        }
        .sheet(isPresented: $showSignUp) {
            SignUpView()
                .environment(appState)
                .presentationBackground(.regularMaterial)
        }
        .sheet(item: $googleSignupContext) { ctx in
            GoogleSignupCompletionView(context: ctx) {
                googleSignupContext = nil
            }
            .environment(appState)
            .presentationBackground(.regularMaterial)
        }
        .alert("Send password reset email?", isPresented: $showResetPrompt) {
            TextField("Email", text: $resetEmail)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
            Button("Send") { sendReset() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("We'll email a link to reset your password.")
        }
    }

    // MARK: - Email + password

    private func signIn() {
        errorMessage = nil
        resetSentForEmail = nil
        isLoading = true
        Task {
            do {
                _ = try await appState.authRepo.signIn(email: email, password: password)
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }

    private func sendReset() {
        let target = resetEmail.trimmingCharacters(in: .whitespaces)
        guard !target.isEmpty else { return }
        errorMessage = nil
        Task {
            do {
                try await appState.authRepo.sendPasswordReset(email: target)
                resetSentForEmail = target
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    // MARK: - Google sign-in

    private func signInWithGoogle() {
        errorMessage = nil
        resetSentForEmail = nil
        isGoogleLoading = true
        Task {
            do {
                let result = try await GoogleSignInService.signIn()
                // Decide between fully signed in vs needs Firestore profile.
                // Mirrors Android's `checkAndCreateUserInFirestore`.
                let existing = try? await appState.userRepo.get(id: result.user.uid)
                if existing != nil {
                    // Existing user — AppState's auth listener will swap the
                    // RootView to the dashboard automatically.
                } else {
                    // Brand-new user — collect phone + gender, then create
                    // the Firestore document.
                    let (first, last) = Self.split(name: result.displayName)
                    googleSignupContext = GoogleSignupContext(
                        uid: result.user.uid,
                        email: result.email ?? "",
                        firstName: first,
                        lastName: last
                    )
                }
            } catch {
                let ns = error as NSError
                if ns.domain == "com.google.GIDSignIn", ns.code == -5 {
                    // User cancelled — no need to show an error.
                } else {
                    errorMessage = error.localizedDescription
                }
            }
            isGoogleLoading = false
        }
    }

    private static func split(name: String?) -> (first: String, last: String) {
        guard let name, !name.isEmpty else { return ("", "") }
        let parts = name.split(separator: " ").map(String.init)
        let first = parts.first ?? ""
        let last  = parts.dropFirst().joined(separator: " ")
        return (first, last)
    }
}

// MARK: - Google sign-up completion

struct GoogleSignupContext: Identifiable, Hashable {
    var id: String { uid }
    let uid: String
    let email: String
    let firstName: String
    let lastName: String
}

private struct GoogleSignupCompletionView: View {
    let context: GoogleSignupContext
    let onDone: () -> Void

    @Environment(AppState.self) private var appState
    @Environment(\.dismiss) private var dismiss

    @State private var firstName: String
    @State private var lastName: String
    @State private var phone: String = ""
    @State private var gender: String = "Male"
    @State private var isSaving = false
    @State private var errorMessage: String?

    init(context: GoogleSignupContext, onDone: @escaping () -> Void) {
        self.context = context
        self.onDone = onDone
        _firstName = State(initialValue: context.firstName)
        _lastName  = State(initialValue: context.lastName)
    }

    private var isValid: Bool { !firstName.isEmpty && !phone.isEmpty }

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.backgroundGradient.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 18) {
                        Image(systemName: "person.badge.shield.checkmark.fill")
                            .font(.system(size: 56))
                            .foregroundStyle(Brand.primary)
                            .padding(.top, 16)
                        Text("Finish setting up").font(Typography.title)
                        Text("Signing in with **\(context.email)**. Just a couple more details.")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal)

                        GlassCard {
                            VStack(spacing: 14) {
                                HStack(spacing: 12) {
                                    GlassTextField(placeholder: "First name", text: $firstName, icon: "person.fill")
                                    GlassTextField(placeholder: "Last name",  text: $lastName,  icon: "person.fill")
                                }
                                GlassTextField(placeholder: "Phone", text: $phone, icon: "phone.fill", keyboardType: .phonePad, contentType: .telephoneNumber)
                                Picker("Gender", selection: $gender) {
                                    Text("Male").tag("Male")
                                    Text("Female").tag("Female")
                                    Text("Other").tag("Other")
                                }
                                .pickerStyle(.segmented)
                                if let errorMessage {
                                    Text(errorMessage).font(.caption).foregroundStyle(.red)
                                }
                            }
                        }
                        .padding(.horizontal)

                        PrimaryButton(title: "Save and continue", icon: "checkmark.circle.fill", isLoading: isSaving) {
                            save()
                        }
                        .disabled(!isValid || isSaving)
                        .opacity(isValid ? 1 : 0.5)
                        .padding(.horizontal)
                    }
                    .padding(.bottom)
                }
            }
            .navigationTitle("Finish Sign-Up")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        try? Auth.auth().signOut()
                        GoogleSignInService.signOut()
                        dismiss()
                        onDone()
                    }
                }
            }
        }
    }

    private func save() {
        errorMessage = nil
        isSaving = true
        Task {
            do {
                let model = SecurityUser(
                    id: context.uid,
                    firstName: firstName,
                    lastName: lastName,
                    email: context.email,
                    phone: phone,
                    gender: gender,
                    vaultSalt: VaultCryptoManager.generateSalt()
                )
                try await appState.userRepo.create(model)
                dismiss()
                onDone()
            } catch {
                errorMessage = error.localizedDescription
            }
            isSaving = false
        }
    }
}

#Preview {
    LoginView()
        .environment(AppState())
}
