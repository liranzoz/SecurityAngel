import SwiftUI

enum AppTab: Hashable {
    case home, scanner, ai, vault, family
}

struct RootView: View {
    @State private var selectedTab: AppTab = .home
    @State private var showMenu = false
    @State private var isAuthenticated = true

    var body: some View {
        Group {
            if isAuthenticated {
                mainTabs
            } else {
                LoginView(onAuthenticated: { isAuthenticated = true })
            }
        }
    }

    private var mainTabs: some View {
        TabView(selection: $selectedTab) {
            Tab("Home", systemImage: "house.fill", value: AppTab.home) {
                NavigationStack { DashboardView(showMenu: $showMenu) }
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
                onSignOut: { showMenu = false; isAuthenticated = false }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .presentationBackground(.regularMaterial)
        }
    }
}

#Preview {
    RootView()
}
