import SwiftUI

struct LoginView: View {
    let onAuthenticated: () -> Void

    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var showSignUp = false

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

                            PrimaryButton(title: "Sign In", icon: "arrow.right", isLoading: isLoading) {
                                isLoading = true
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                                    isLoading = false
                                    onAuthenticated()
                                }
                            }

                            HStack {
                                Rectangle().fill(.white.opacity(0.25)).frame(height: 1)
                                Text("or").font(.caption).foregroundStyle(.secondary)
                                Rectangle().fill(.white.opacity(0.25)).frame(height: 1)
                            }

                            Button {
                                onAuthenticated()
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: "g.circle.fill").foregroundStyle(.red)
                                    Text("Continue with Google").font(.headline)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .foregroundStyle(.primary)
                                .liquidGlassCapsule()
                            }
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
            SignUpView(onCompleted: {
                showSignUp = false
                onAuthenticated()
            })
            .presentationBackground(.regularMaterial)
        }
    }
}

#Preview {
    LoginView(onAuthenticated: {})
}
