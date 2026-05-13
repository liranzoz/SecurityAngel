import SwiftUI

struct FamilySafetyView: View {
    @Binding var showMenu: Bool
    @Environment(AppState.self) private var appState
    @State private var showAddMember = false
    @State private var showJoin = false

    private var members: [SecurityUser] { appState.familyMembers }
    private var family: SecurityFamily? { appState.family }
    private var adminId: String { family?.adminId ?? "" }

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    ScreenTitleBar(
                        title: "Family Safety",
                        onMenu: { showMenu = true },
                        trailing: family == nil ? nil : AnyView(
                            Button { showAddMember = true } label: {
                                Image(systemName: "person.crop.circle.badge.plus")
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(.primary)
                                    .frame(width: 40, height: 40)
                                    .liquidGlassCard(cornerRadius: 12)
                            }
                        )
                    )
                    .padding(.top, 8)

                    if family == nil {
                        noFamilyCard
                    } else {
                        summaryCard
                        membersSection
                    }
                    Spacer(minLength: 80)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
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
    }

    // MARK: - Cards

    private var summaryCard: some View {
        GlassCard {
            HStack(spacing: 16) {
                LottieAnimation(animation: .familyAvatar)
                    .frame(width: 84, height: 84)
                VStack(alignment: .leading, spacing: 4) {
                    Text(family?.name ?? "Family")
                        .font(Typography.title)
                    Text("\(members.count) member\(members.count == 1 ? "" : "s")")
                        .font(.caption).foregroundStyle(.secondary)
                    let alerts = appState.familyAlertCount
                    if alerts == 0 {
                        StatusPill(text: "All Safe", kind: .safe)
                    } else {
                        StatusPill(text: "\(alerts) Alert\(alerts == 1 ? "" : "s")", kind: .unsafe)
                    }
                }
                Spacer()
            }
        }
        .padding(.horizontal)
    }

    private var noFamilyCard: some View {
        GlassCard {
            VStack(spacing: 14) {
                Image(systemName: "person.3.sequence.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Brand.primary)
                Text("You're not in a family yet")
                    .font(Typography.title)
                Text("Create one by inviting a member, or join an existing family with the 6-digit code an admin sent you.")
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
        .padding(.top, 12)
    }

    private var membersSection: some View {
        VStack(spacing: 10) {
            SectionHeader("Family Members").padding(.horizontal, 24)
            ForEach(members) { member in
                HStack(spacing: 14) {
                    LottieAvatar(gender: member.gender, size: 52)
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 6) {
                            Text(member.fullName)
                                .font(.subheadline.weight(.semibold))
                            if member.id == adminId {
                                Text("Admin")
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 6).padding(.vertical, 2)
                                    .background(Brand.primary.opacity(0.15), in: Capsule())
                                    .foregroundStyle(Brand.primary)
                            }
                        }
                        Text(member.email)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    if member.riskCount > 0 {
                        StatusPill(text: "\(member.riskCount) Breach\(member.riskCount == 1 ? "" : "es")", kind: .unsafe)
                    } else {
                        StatusPill(text: "Safe", kind: .safe)
                    }
                }
                .padding(12)
                .liquidGlassCard(cornerRadius: 18)
                .padding(.horizontal)
            }
        }
    }
}

#Preview {
    FamilySafetyView(showMenu: .constant(false))
        .environment(AppState())
}
