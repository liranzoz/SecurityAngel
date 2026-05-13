import SwiftUI

struct SettingsView: View {
    var onSignOut: () -> Void

    @AppStorage("dark_mode") private var darkMode = false
    @AppStorage("biometric_enabled") private var biometricEnabled = true
    @AppStorage("prevent_screenshots") private var preventScreenshots = false

    private let user = MockData.currentUser

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    profileCard
                    settingsCard
                    aboutCard
                    Button(role: .destructive, action: onSignOut) {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                            Text("Sign Out").bold()
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .foregroundStyle(.red)
                        .liquidGlassCard(cornerRadius: 18)
                    }
                    .padding(.horizontal)

                    Text("Liran Zozulya")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
                    Spacer(minLength: 40)
                }
                .padding(.top, 8)
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var profileCard: some View {
        GlassCard(padding: 20, tint: Brand.primary.opacity(0.12)) {
            HStack(spacing: 14) {
                Image(systemName: user.avatar)
                    .font(.system(size: 36))
                    .foregroundStyle(.white)
                    .frame(width: 60, height: 60)
                    .background(Brand.headerGradient, in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(user.fullName).font(Typography.title)
                    Text(user.email).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
        .padding(.horizontal)
    }

    private var settingsCard: some View {
        VStack(spacing: 10) {
            SectionHeader("Preferences").padding(.horizontal, 24)
            VStack(spacing: 0) {
                settingsRow(icon: "moon.fill", title: "Dark Mode", isOn: $darkMode)
                Divider().opacity(0.3)
                settingsRow(icon: "faceid", title: "Biometric Unlock", isOn: $biometricEnabled)
                Divider().opacity(0.3)
                settingsRow(icon: "camera.fill", title: "Prevent Screenshots", isOn: $preventScreenshots)
                Divider().opacity(0.3)
                NavigationLink {
                    FamilyManagementView()
                } label: {
                    settingsLinkRow(icon: "person.3.fill", title: "My Family")
                }
                Divider().opacity(0.3)
                NavigationLink {
                    DevicePostureView()
                } label: {
                    settingsLinkRow(icon: "shield.lefthalf.filled", title: "Device Posture")
                }
                Divider().opacity(0.3)
                NavigationLink {
                    SecurityLogView()
                } label: {
                    settingsLinkRow(icon: "list.bullet.rectangle", title: "Activity Log")
                }
                Divider().opacity(0.3)
                NavigationLink {
                    PasswordGeneratorView()
                } label: {
                    settingsLinkRow(icon: "wand.and.stars", title: "Password Generator")
                }
            }
            .liquidGlassCard(cornerRadius: 18)
            .padding(.horizontal)
        }
    }

    private var aboutCard: some View {
        VStack(spacing: 10) {
            SectionHeader("About").padding(.horizontal, 24)
            VStack(spacing: 0) {
                infoRow(icon: "info.circle.fill", title: "Version", value: "1.0 (iOS)")
                Divider().opacity(0.3)
                infoRow(icon: "lock.shield.fill", title: "Privacy Policy", value: "›")
                Divider().opacity(0.3)
                infoRow(icon: "doc.text.fill", title: "Terms of Service", value: "›")
            }
            .liquidGlassCard(cornerRadius: 18)
            .padding(.horizontal)
        }
    }

    private func settingsRow(icon: String, title: String, isOn: Binding<Bool>) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .foregroundStyle(Brand.primary)
                .frame(width: 28)
            Text(title)
            Spacer()
            Toggle("", isOn: isOn).tint(Brand.primary).labelsHidden()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func settingsLinkRow(icon: String, title: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon).foregroundStyle(Brand.primary).frame(width: 28)
            Text(title).foregroundStyle(.primary)
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func infoRow(icon: String, title: String, value: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon).foregroundStyle(Brand.primary).frame(width: 28)
            Text(title)
            Spacer()
            Text(value).foregroundStyle(.secondary).font(.callout)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

#Preview {
    NavigationStack { SettingsView(onSignOut: {}) }
}
