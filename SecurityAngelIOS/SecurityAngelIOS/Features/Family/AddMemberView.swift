import SwiftUI

struct AddMemberView: View {
    @Environment(AppState.self) private var appState
    @Environment(\.dismiss) private var dismiss

    @State private var email = ""
    @State private var generatedCode = ""
    @State private var isWorking = false
    @State private var errorMessage: String?
    @State private var showShare = false

    private var shareText: String {
        """
        Hey! I want to add you to my family in Security Angel.

        1. Download the app.
        2. Register with email: \(email)
        3. Use this secure code to join: *\(generatedCode)*

        Stay safe!
        """
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.backgroundGradient.ignoresSafeArea()
                VStack(spacing: 20) {
                    Image(systemName: "person.crop.circle.badge.plus")
                        .font(.system(size: 72))
                        .foregroundStyle(Brand.primary)
                        .padding(28)
                        .liquidGlass(in: Circle())
                        .padding(.top, 20)

                    Text("Invite a family member")
                        .font(Typography.title)
                    Text("Enter their email — we'll generate a secure 6-digit code they can use to join.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    GlassCard {
                        VStack(spacing: 14) {
                            GlassTextField(
                                placeholder: "Email", text: $email,
                                icon: "envelope.fill",
                                keyboardType: .emailAddress,
                                contentType: .emailAddress
                            )
                            if !generatedCode.isEmpty {
                                HStack(spacing: 10) {
                                    ForEach(Array(generatedCode), id: \.self) { ch in
                                        Text(String(ch))
                                            .font(.system(.title, design: .rounded, weight: .bold))
                                            .frame(width: 42, height: 52)
                                            .liquidGlassCard(cornerRadius: 12)
                                    }
                                }
                            }
                            if let errorMessage {
                                Text(errorMessage).font(.caption).foregroundStyle(.red)
                            }
                            PrimaryButton(
                                title: generatedCode.isEmpty ? "Send Invitation" : "Share",
                                icon: generatedCode.isEmpty ? "paperplane.fill" : "square.and.arrow.up",
                                isLoading: isWorking
                            ) {
                                if generatedCode.isEmpty {
                                    createInvitation()
                                } else {
                                    showShare = true
                                }
                            }
                            .disabled(email.isEmpty || isWorking)
                            .opacity(email.isEmpty ? 0.5 : 1)
                        }
                    }
                    .padding(.horizontal)

                    Spacer()
                }
            }
            .navigationTitle("Add Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .sheet(isPresented: $showShare) {
                ShareLink(item: shareText) {
                    Label("Share Invitation", systemImage: "square.and.arrow.up")
                }
                .padding()
                .presentationDetents([.height(200)])
            }
        }
    }

    private func createInvitation() {
        errorMessage = nil
        isWorking = true
        Task {
            do {
                guard let currentUser = appState.currentUser else {
                    errorMessage = "Not signed in"
                    isWorking = false
                    return
                }
                let familyId: String
                if let existing = currentUser.familyId, !existing.isEmpty {
                    familyId = existing
                    // Best-effort link the invitee to the existing family
                    try? await appState.familyRepo.addMember(familyId: existing, byEmail: email)
                } else {
                    let newId = try await appState.familyRepo.createFamily(
                        admin: currentUser,
                        name: "\(currentUser.lastName.isEmpty ? "My" : currentUser.lastName) Family"
                    )
                    familyId = newId
                    try? await appState.familyRepo.addMember(familyId: newId, byEmail: email)
                }
                let code = try await appState.invitationRepo.create(email: email, familyId: familyId)
                generatedCode = code
                try? await appState.logger.logEvent(
                    uid: currentUser.id, eventType: .memberAdded,
                    description: "Invited \(email) to the family."
                )
            } catch {
                if case FamilyRepository.FamilyError.alreadyInFamily = error {
                    errorMessage = "User already belongs to another family."
                } else if case FamilyRepository.FamilyError.userNotFound = error {
                    // It's OK — they may not have an account yet. The invitation
                    // will still be created so they can use the code on signup.
                    do {
                        guard let currentUser = appState.currentUser else { return }
                        let familyId: String
                        if let existing = currentUser.familyId, !existing.isEmpty {
                            familyId = existing
                        } else {
                            familyId = try await appState.familyRepo.createFamily(
                                admin: currentUser,
                                name: "\(currentUser.lastName.isEmpty ? "My" : currentUser.lastName) Family"
                            )
                        }
                        let code = try await appState.invitationRepo.create(email: email, familyId: familyId)
                        generatedCode = code
                    } catch {
                        errorMessage = error.localizedDescription
                    }
                } else {
                    errorMessage = error.localizedDescription
                }
            }
            isWorking = false
        }
    }
}

#Preview {
    AddMemberView()
        .environment(AppState())
}
