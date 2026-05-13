import SwiftUI

struct MenuSheet: View {
    var onSelectGenerator: () -> Void
    var onSignOut: () -> Void

    @Environment(AppState.self) private var appState

    private var user: SecurityUser? { appState.currentUser }

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
                LottieAvatar(gender: user?.gender ?? "", size: 56)
                VStack(alignment: .leading, spacing: 2) {
                    Text(user?.fullName ?? "Signed in")
                        .font(Typography.sectionTitle)
                    Text(user?.email ?? "")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
        .padding(.horizontal)
    }

    private var quickActions: some View {
        HStack(spacing: 12) {
            NavigationLink { PasswordGeneratorView() } label: { quickTile(icon: "wand.and.stars", title: "Generator") }
            NavigationLink { DevicePostureView() } label: { quickTile(icon: "shield.lefthalf.filled", title: "Posture") }
            NavigationLink { SecurityLogView() } label: { quickTile(icon: "list.bullet.rectangle", title: "Logs") }
        }
        .padding(.horizontal)
        .buttonStyle(.plain)
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
        .environment(AppState())
}
