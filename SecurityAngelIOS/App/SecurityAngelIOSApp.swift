import SwiftUI

@main
struct SecurityAngelIOSApp: App {
    @AppStorage("dark_mode") private var darkMode = false

    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(darkMode ? .dark : nil)
                .tint(Brand.primary)
        }
    }
}
