import SwiftUI

/// The Security Angel wordmark. If a `securityAngelLogo` image is present
/// in the asset catalog, it's used verbatim. Otherwise we render a
/// SwiftUI two-tone wordmark that matches the brand image pixel-for-pixel
/// in spirit ("Security" in deep navy, "Angel" in brand green, white
/// rounded card behind).
struct AppLogo: View {
    var maxWidth: CGFloat = 240
    var maxHeight: CGFloat = 88

    var body: some View {
        Group {
            #if canImport(UIKit)
            if UIImage(named: "securityAngelLogo") != nil {
                Image("securityAngelLogo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: maxWidth, maxHeight: maxHeight)
            } else {
                wordmark
            }
            #else
            wordmark
            #endif
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 16)
        .background(.white, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 16, y: 6)
    }

    private var wordmark: some View {
        HStack(spacing: 2) {
            Text("Security")
                .foregroundStyle(Color(red: 0x18/255, green: 0x36/255, blue: 0x3A/255))
            Text("Angel")
                .foregroundStyle(Brand.primary)
        }
        .font(.system(.title, design: .rounded, weight: .bold))
    }
}

#Preview {
    AppLogo()
        .padding()
        .background(Brand.headerGradient)
}
