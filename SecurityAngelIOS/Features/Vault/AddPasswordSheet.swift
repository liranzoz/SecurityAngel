import SwiftUI

struct AddPasswordSheet: View {
    let onSave: (MockPasswordAccount) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var siteName = ""
    @State private var domain = ""
    @State private var email = ""
    @State private var password = ""

    private var isValid: Bool { !siteName.isEmpty && !password.isEmpty }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    GlassTextField(placeholder: "Site name (e.g. GitHub)", text: $siteName, icon: "globe")
                    GlassTextField(placeholder: "Domain (e.g. github.com)", text: $domain, icon: "link")
                    GlassTextField(placeholder: "Email or username", text: $email, icon: "person.fill", contentType: .username)
                    GlassTextField(placeholder: "Password", text: $password, icon: "lock.fill", isSecure: true, contentType: .newPassword)
                }
                .padding()
            }
            .background(Brand.backgroundGradient.ignoresSafeArea())
            .navigationTitle("Add Password")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let acc = MockPasswordAccount(
                            id: UUID().uuidString,
                            siteName: siteName,
                            email: email,
                            domain: domain,
                            password: password,
                            isLeaked: false
                        )
                        onSave(acc)
                        dismiss()
                    }
                    .disabled(!isValid)
                }
            }
        }
    }
}

#Preview {
    AddPasswordSheet(onSave: { _ in })
}
