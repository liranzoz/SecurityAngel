import SwiftUI

struct FamilyManagementView: View {
    @State private var showAddMember = false
    @State private var showJoin = false
    private let members = MockData.familyMembers
    private let adminId = "u1"

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    headerCard
                    membersList
                    Spacer(minLength: 60)
                }
                .padding(.top, 8)
            }
        }
        .navigationTitle("Family Management")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("Add Member", systemImage: "person.crop.circle.badge.plus") { showAddMember = true }
                    Button("Join with Code", systemImage: "ticket.fill") { showJoin = true }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(isPresented: $showAddMember) {
            AddMemberView()
                .presentationDetents([.medium, .large])
                .presentationBackground(.regularMaterial)
        }
        .sheet(isPresented: $showJoin) {
            JoinFamilyView()
                .presentationDetents([.medium])
                .presentationBackground(.regularMaterial)
        }
    }

    private var headerCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 6) {
                Text("Zoz Family").font(Typography.title)
                Text("\(members.count) members").foregroundStyle(.secondary).font(.subheadline)
            }
        }
        .padding(.horizontal)
    }

    private var membersList: some View {
        VStack(spacing: 10) {
            SectionHeader("Members").padding(.horizontal, 24)
            ForEach(members) { member in
                HStack(spacing: 14) {
                    Image(systemName: member.avatar)
                        .font(.title)
                        .foregroundStyle(Brand.primary)
                        .frame(width: 44, height: 44)
                        .liquidGlass(in: Circle())
                    VStack(alignment: .leading) {
                        HStack(spacing: 6) {
                            Text(member.fullName).font(.subheadline.weight(.semibold))
                            if member.id == adminId {
                                Text("Admin")
                                    .font(.caption.bold())
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(Brand.primary.opacity(0.15), in: Capsule())
                                    .foregroundStyle(Brand.primary)
                            }
                        }
                        Text(member.email).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    if member.id != adminId {
                        Button(role: .destructive) {} label: {
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
}

#Preview {
    NavigationStack { FamilyManagementView() }
}
