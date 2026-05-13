import SwiftUI

struct LoginView: View {
    @Environment(AppState.self) private var appState

    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var showSignUp = false
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            Brand.headerGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 28) {
                    Spacer(minLength: 60)

                    VStack(spacing: 12) {
                        Image(systemName: "shield.lefthalf.filled.badge.checkmark")
                            .font(.system(size: 64, weight: .light))
                            .foregroundStyle(.white)
                            .padding(28)
                            .liquidGlass(in: Circle())

                        Text("Security Angel")
                            .font(.system(.largeTitle, design: .rounded, weight: .bold))
                            .foregroundStyle(.white)
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

                            PrimaryButton(title: "Sign In", icon: "arrow.right", isLoading: isLoading) {
                                signIn()
                            }
                            .disabled(email.isEmpty || password.isEmpty || isLoading)
                            .opacity((email.isEmpty || password.isEmpty) ? 0.5 : 1)
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
    }

    private func signIn() {
        errorMessage = nil
        isLoading = true
        Task {
            do {
                _ = try await appState.authRepo.signIn(email: email, password: password)
                // AppState's auth listener will fire on success and flip the root view.
            } catch let error as AuthRepoError {
                errorMessage = error.localizedDescription
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}

#Preview {
    LoginView()
        .environment(AppState())
}
