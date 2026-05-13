import SwiftUI

struct FamilySafetyView: View {
    @Binding var showMenu: Bool
    @State private var showAddMember = false
    private let members = MockData.familyMembers

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    ScreenTitleBar(
                        title: "Family Safety",
                        onMenu: { showMenu = true },
                        trailing: AnyView(
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

                    summaryCard
                    membersSection
                    Spacer(minLength: 80)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showAddMember) {
            AddMemberView()
                .presentationDetents([.medium, .large])
                .presentationBackground(.regularMaterial)
        }
    }

    private var summaryCard: some View {
        GlassCard {
            HStack(spacing: 16) {
                Image(systemName: "person.3.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(Brand.primary)
                    .padding(14)
                    .liquidGlass(in: Circle(), tint: Brand.primary.opacity(0.15))
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(members.count) members")
                        .font(Typography.title)
                    let alertCount = members.reduce(0) { $0 + $1.riskCount }
                    if alertCount == 0 {
                        StatusPill(text: "All Safe", kind: .safe)
                    } else {
                        StatusPill(text: "\(alertCount) Alerts", kind: .unsafe)
                    }
                }
                Spacer()
            }
        }
        .padding(.horizontal)
    }

    private var membersSection: some View {
        VStack(spacing: 10) {
            SectionHeader("Family Members").padding(.horizontal, 24)
            ForEach(members) { member in
                HStack(spacing: 14) {
                    Image(systemName: member.avatar)
                        .font(.title)
                        .foregroundStyle(Brand.primary)
                        .frame(width: 48, height: 48)
                        .liquidGlass(in: Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(member.fullName)
                            .font(.subheadline.weight(.semibold))
                        Text(member.email)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    if member.riskCount > 0 {
                        StatusPill(text: "\(member.riskCount) Breaches", kind: .unsafe)
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
}
