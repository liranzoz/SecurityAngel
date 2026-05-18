import SwiftUI

enum AppTab: Hashable {
    case home, scanner, ai, vault, family
}

struct RootView: View {
    @Environment(AppState.self) private var appState
    @State private var selectedTab: AppTab = .home
    @State private var showMenu = false

    var body: some View {
        Group {
            if !appState.firebaseAvailable {
                FirebaseSetupView()
            } else if appState.firebaseUserId == nil {
                LoginView()
            } else if appState.currentUser == nil {
                LoadingUserView()
            } else {
                mainTabs
            }
        }
    }

    private var mainTabs: some View {
        TabView(selection: $selectedTab) {
            Tab("Home", systemImage: "house.fill", value: AppTab.home) {
                NavigationStack {
                    DashboardView(showMenu: $showMenu, onSelectTab: { selectedTab = $0 })
                }
            }
            Tab("Scanner", systemImage: "shield.lefthalf.filled", value: AppTab.scanner) {
                NavigationStack { URLScannerView(showMenu: $showMenu) }
            }
            Tab("AI", systemImage: "sparkles", value: AppTab.ai) {
                NavigationStack { AIChatView(showMenu: $showMenu) }
            }
            Tab("Vault", systemImage: "lock.shield.fill", value: AppTab.vault) {
                NavigationStack { PasswordVaultView(showMenu: $showMenu) }
            }
            Tab("Family", systemImage: "person.3.fill", value: AppTab.family) {
                NavigationStack { FamilySafetyView(showMenu: $showMenu) }
            }
        }
        .sheet(isPresented: $showMenu) {
            MenuSheet(
                onSelectGenerator: { showMenu = false; selectedTab = .vault },
                onSignOut: { showMenu = false; appState.signOut() }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .presentationBackground(.regularMaterial)
        }
    }
}

// MARK: - Setup / loading states

struct FirebaseSetupView: View {
    var body: some View {
        ZStack {
            Brand.headerGradient.ignoresSafeArea()
            VStack(spacing: 20) {
                Image(systemName: "exclamationmark.icloud.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(.white)
                    .padding(28)
                    .liquidGlass(in: Circle())
                Text("Firebase isn't configured")
                    .font(Typography.title)
                    .foregroundStyle(.white)
                VStack(alignment: .leading, spacing: 8) {
                    Text("To enable Security Angel:")
                    Text("1. Open your Firebase console.").bold()
                    Text("2. Add an iOS app with bundle id\n   com.zoz.SecurityAngelIOS.")
                    Text("3. Download GoogleService-Info.plist.").bold()
                    Text("4. Drop it into the project's\n   SecurityAngelIOS/ folder and rebuild.")
                }
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.95))
                .padding()
                .liquidGlassCard(cornerRadius: 18)
                .padding(.horizontal, 24)
            }
        }
    }
}

struct LoadingUserView: View {
    @Environment(AppState.self) private var appState

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            VStack(spacing: 16) {
                ProgressView().tint(Brand.primary).controlSize(.large)
                Text("Loading your profile…")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Button("Cancel") { appState.signOut() }
                    .padding(.top, 24)
                    .foregroundStyle(Brand.primary)
            }
        }
    }
}

#Preview {
    RootView()
        .environment(AppState())
}
