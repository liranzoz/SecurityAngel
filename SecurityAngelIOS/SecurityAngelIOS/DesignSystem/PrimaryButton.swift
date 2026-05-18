import SwiftUI

struct PrimaryButton: View {
    let title: String
    var icon: String? = nil
    var isLoading: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView().tint(.white)
                } else if let icon {
                    Image(systemName: icon)
                }
                Text(title).font(.headline)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .foregroundStyle(.white)
            .background(Brand.headerGradient, in: Capsule())
            .shadow(color: Brand.primary.opacity(0.4), radius: 12, y: 6)
        }
        .disabled(isLoading)
    }
}

struct SecondaryButton: View {
    let title: String
    var icon: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon { Image(systemName: icon) }
                Text(title).font(.headline)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .foregroundStyle(Brand.primary)
            .liquidGlassCapsule()
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        PrimaryButton(title: "Sign In", icon: "lock.fill") {}
        PrimaryButton(title: "Loading", isLoading: true) {}
        SecondaryButton(title: "Paste from Clipboard", icon: "doc.on.clipboard") {}
    }
    .padding()
    .background(Brand.backgroundGradient)
}
