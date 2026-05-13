import SwiftUI

struct SignUpView: View {
    let onCompleted: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var password = ""
    @State private var gender: String = "Male"
    @State private var didSendVerification = false

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
                }
            }

            PrimaryButton(title: "Create Account", icon: "checkmark.circle.fill") {
                didSendVerification = true
            }
            .disabled(!isValid)
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
                Text("We sent a link to\n\(email)\n\nClick it to verify your account.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 32)

            PrimaryButton(title: "I've verified", icon: "checkmark.circle.fill") {
                onCompleted()
            }
            .padding(.horizontal)

            Button("Resend verification email") {}
                .foregroundStyle(Brand.primary)
        }
        .padding()
    }
}

#Preview {
    SignUpView(onCompleted: {})
}
