import SwiftUI

struct MenuSheet: View {
    var onSelectGenerator: () -> Void
    var onSignOut: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    profileHeader
                    quickActions
                    settingsList
                    Spacer(minLength: 40)
                }
                .padding(.top, 8)
            }
            .background(Brand.backgroundGradient.ignoresSafeArea())
            .navigationTitle("Menu")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        SettingsView(onSignOut: onSignOut)
                    } label: {
                        Image(systemName: "gearshape.fill")
                    }
                }
            }
        }
    }

    private var profileHeader: some View {
        GlassCard {
            HStack(spacing: 14) {
                Image(systemName: MockData.currentUser.avatar)
                    .font(.system(size: 36))
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(Brand.headerGradient, in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(MockData.currentUser.fullName)
                        .font(Typography.sectionTitle)
                    Text(MockData.currentUser.email)
                        .font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
        .padding(.horizontal)
    }

    private var quickActions: some View {
        HStack(spacing: 12) {
            quickAction(icon: "wand.and.stars", title: "Generator") {
                NavigationLink { PasswordGeneratorView() } label: { quickTile(icon: "wand.and.stars", title: "Generator") }
            }
            quickAction(icon: "shield.lefthalf.filled", title: "Posture") {
                NavigationLink { DevicePostureView() } label: { quickTile(icon: "shield.lefthalf.filled", title: "Posture") }
            }
            quickAction(icon: "list.bullet.rectangle", title: "Logs") {
                NavigationLink { SecurityLogView() } label: { quickTile(icon: "list.bullet.rectangle", title: "Logs") }
            }
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private func quickAction<Content: View>(icon: String, title: String, @ViewBuilder content: () -> Content) -> some View {
        content().buttonStyle(.plain)
    }

    private func quickTile(icon: String, title: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(Brand.primary)
            Text(title).font(.caption.bold()).foregroundStyle(.primary)
        }
        .frame(maxWidth: .infinity, minHeight: 80)
        .liquidGlassCard(cornerRadius: 18)
    }

    private var settingsList: some View {
        VStack(spacing: 0) {
            NavigationLink {
                FamilyManagementView()
            } label: { row(icon: "person.3.fill", title: "Family Management") }
            Divider().opacity(0.3)
            NavigationLink {
                SettingsView(onSignOut: onSignOut)
            } label: { row(icon: "gearshape.fill", title: "Settings") }
        }
        .liquidGlassCard(cornerRadius: 18)
        .padding(.horizontal)
    }

    private func row(icon: String, title: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon).foregroundStyle(Brand.primary).frame(width: 28)
            Text(title).foregroundStyle(.primary)
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}

#Preview {
    MenuSheet(onSelectGenerator: {}, onSignOut: {})
}
