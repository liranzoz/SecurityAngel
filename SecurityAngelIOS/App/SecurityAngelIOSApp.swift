import SwiftUI

@main
struct SecurityAngelIOSApp: App {
    @AppStorage("dark_mode") private var darkMode = false
    @State private var appState = AppState()

    init() {
        _ = FirebaseSupport.isConfigured
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(appState)
                .preferredColorScheme(darkMode ? .dark : nil)
                .tint(Brand.primary)
        }
    }
}
