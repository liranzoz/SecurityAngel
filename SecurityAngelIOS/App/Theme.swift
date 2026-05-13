import SwiftUI

enum Brand {
    static let primary       = Color(red: 0x15/255, green: 0xB7/255, blue: 0x9F/255)
    static let primaryDark   = Color(red: 0x0F/255, green: 0x8E/255, blue: 0x7A/255)
    static let accent        = Color(red: 0x34/255, green: 0xD3/255, blue: 0x99/255)
    static let lightGreen    = Color(red: 0xD1/255, green: 0xFA/255, blue: 0xE5/255)

    static let safe          = Color(red: 0x06/255, green: 0x5F/255, blue: 0x46/255)
    static let safeBg        = Color(red: 0xD1/255, green: 0xFA/255, blue: 0xE5/255)
    static let unsafe        = Color(red: 0x99/255, green: 0x1B/255, blue: 0x1B/255)
    static let unsafeBg      = Color(red: 0xFE/255, green: 0xE2/255, blue: 0xE2/255)
    static let warning       = Color(red: 0xC8/255, green: 0x98/255, blue: 0x08/255)
    static let warningBg     = Color(red: 0xFE/255, green: 0xF3/255, blue: 0xC7/255)
    static let iconBlue      = Color(red: 0x19/255, green: 0x76/255, blue: 0xD2/255)
    static let iconBlueBg    = Color(red: 0xE3/255, green: 0xF2/255, blue: 0xFD/255)

    static let headerGradient = LinearGradient(
        colors: [primary, primaryDark],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let backgroundGradient = LinearGradient(
        colors: [
            Color(red: 0xF8/255, green: 0xF9/255, blue: 0xFA/255),
            Color(red: 0xEC/255, green: 0xFD/255, blue: 0xF5/255)
        ],
        startPoint: .top,
        endPoint: .bottom
    )
}

enum Typography {
    static let display    = Font.system(.largeTitle, design: .rounded, weight: .bold)
    static let title      = Font.system(.title2, design: .rounded, weight: .bold)
    static let sectionTitle = Font.system(.headline, design: .rounded, weight: .semibold)
    static let body       = Font.system(.body, design: .default)
    static let caption    = Font.system(.footnote, design: .default)
    static let scoreValue = Font.system(size: 56, weight: .light, design: .rounded)
}
