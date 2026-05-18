import SwiftUI

struct FamilyManagementView: View {
    @Environment(AppState.self) private var appState
    @State private var showAddMember = false
    @State private var showJoin = false
    @State private var pendingRemoval: SecurityUser? = nil

    private var members: [SecurityUser] { appState.familyMembers }
    private var family: SecurityFamily? { appState.family }
    private var isAdmin: Bool { appState.isAdmin }
    private var adminId: String { family?.adminId ?? "" }
    private var currentUserId: String { appState.currentUser?.id ?? "" }

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    if family == nil {
                        joinPrompt
                    } else {
                        headerCard
                        membersList
                    }
                    Spacer(minLength: 60)
                }
                .padding(.top, 8)
            }
        }
        .navigationTitle("Family Management")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if family != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("Add Member", systemImage: "person.crop.circle.badge.plus") { showAddMember = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .sheet(isPresented: $showAddMember) {
            AddMemberView()
                .environment(appState)
                .presentationDetents([.medium, .large])
                .presentationBackground(.regularMaterial)
        }
        .sheet(isPresented: $showJoin) {
            JoinFamilyView()
                .environment(appState)
                .presentationDetents([.medium])
                .presentationBackground(.regularMaterial)
        }
        .confirmationDialog(
            pendingRemoval.map { "Remove \($0.firstName) from family?" } ?? "Remove?",
            isPresented: Binding(get: { pendingRemoval != nil }, set: { if !$0 { pendingRemoval = nil } }),
            presenting: pendingRemoval
        ) { member in
            Button("Remove", role: .destructive) { confirmRemove(member) }
            Button("Cancel", role: .cancel) {}
        }
    }

    private var headerCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 6) {
                Text(family?.name ?? "Family").font(Typography.title)
                Text("\(members.count) member\(members.count == 1 ? "" : "s")")
                    .foregroundStyle(.secondary)
                    .font(.subheadline)
            }
        }
        .padding(.horizontal)
    }

    private var joinPrompt: some View {
        GlassCard {
            VStack(spacing: 14) {
                Image(systemName: "ticket.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Brand.primary)
                Text("Join or create a family")
                    .font(Typography.title)
                Text("Tap below to enter an invitation code sent by an admin, or add a member to start a new family.")
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                HStack(spacing: 12) {
                    PrimaryButton(title: "Invite", icon: "person.badge.plus") { showAddMember = true }
                    SecondaryButton(title: "Join", icon: "ticket.fill") { showJoin = true }
                }
            }
        }
        .padding(.horizontal)
    }

    private var membersList: some View {
        VStack(spacing: 10) {
            SectionHeader("Members").padding(.horizontal, 24)
            ForEach(members) { member in
                HStack(spacing: 14) {
                    LottieAvatar(gender: member.gender, size: 48)
                    VStack(alignment: .leading) {
                        HStack(spacing: 6) {
                            Text(member.fullName + (member.id == currentUserId ? " (You)" : ""))
                                .font(.subheadline.weight(.semibold))
                            if member.id == adminId {
                                Text("Admin")
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 6).padding(.vertical, 2)
                                    .background(Brand.primary.opacity(0.15), in: Capsule())
                                    .foregroundStyle(Brand.primary)
                            }
                        }
                        Text(member.email).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    if isAdmin && member.id != adminId {
                        Button { pendingRemoval = member } label: {
                            Image(systemName: "person.crop.circle.badge.minus")
                                .foregroundStyle(.red)
                        }
                    }
                }
                .padding(12)
                .liquidGlassCard(cornerRadius: 18)
                .padding(.horizontal)
            }
        }
    }

    private func confirmRemove(_ member: SecurityUser) {
        guard let familyId = family?.id else { return }
        Task {
            try? await appState.familyRepo.removeMember(familyId: familyId, memberId: member.id)
        }
        pendingRemoval = nil
    }
}

#Preview {
    NavigationStack { FamilyManagementView() }
        .environment(AppState())
}
