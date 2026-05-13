import SwiftUI
import FirebaseAuth

@main
struct SecurityAngelIOSApp: App {
    @AppStorage("dark_mode") private var darkMode = false
    @State private var appState = AppState()

    init() {
        _ = FirebaseSupport.isConfigured
        // Make the Firebase Auth session live in the shared keychain group
        // so the AutoFill extension sees the same currentUser without
        // requiring a second sign-in.
        if let group = SharedKeychain.fullGroupID {
            try? Auth.auth().useUserAccessGroup(group)
        }
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
