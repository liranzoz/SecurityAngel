import SwiftUI

struct JoinFamilyView: View {
    @Environment(AppState.self) private var appState
    @Environment(\.dismiss) private var dismiss

    @State private var code = ""
    @State private var errorMessage: String?
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.backgroundGradient.ignoresSafeArea()
                VStack(spacing: 20) {
                    Image(systemName: "ticket.fill")
                        .font(.system(size: 64))
                        .foregroundStyle(Brand.primary)
                        .padding(24)
                        .liquidGlass(in: Circle())
                        .padding(.top, 16)

                    Text("Join a Family")
                        .font(Typography.title)
                    Text("Enter the 6-digit code you received.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    GlassCard {
                        VStack(spacing: 12) {
                            GlassTextField(placeholder: "6-digit code", text: $code, icon: "number", keyboardType: .numberPad)
                            if let errorMessage {
                                Text(errorMessage).font(.caption).foregroundStyle(.red)
                            }
                            HStack(spacing: 10) {
                                Button {
                                    if let str = UIPasteboard.general.string {
                                        code = str.filter(\.isNumber)
                                    }
                                } label: {
                                    HStack {
                                        Image(systemName: "doc.on.clipboard")
                                        Text("Paste")
                                    }
                                    .font(.callout.bold())
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .foregroundStyle(.primary)
                                    .liquidGlassCapsule()
                                }
                                PrimaryButton(title: "Join", isLoading: isWorking) {
                                    join()
                                }
                                .disabled(isWorking || code.count != 6)
                            }
                        }
                    }
                    .padding(.horizontal)
                    Spacer()
                }
            }
            .navigationTitle("Join Family")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func join() {
        errorMessage = nil
        guard let currentUser = appState.currentUser, !currentUser.email.isEmpty else {
            errorMessage = "Not signed in."
            return
        }
        isWorking = true
        Task {
            do {
                guard let familyId = try await appState.invitationRepo.verify(email: currentUser.email, code: code) else {
                    errorMessage = "Invalid code or no invitation found for \(currentUser.email)."
                    isWorking = false
                    return
                }
                try await appState.familyRepo.joinFamily(
                    userId: currentUser.id,
                    familyId: familyId,
                    invitationEmail: currentUser.email
                )
                try? await appState.logger.logEvent(
                    uid: currentUser.id, eventType: .memberAdded,
                    description: "Member \(currentUser.email) joined the family."
                )
                dismiss()
            } catch {
                errorMessage = error.localizedDescription
            }
            isWorking = false
        }
    }
}

#Preview {
    JoinFamilyView()
        .environment(AppState())
}
