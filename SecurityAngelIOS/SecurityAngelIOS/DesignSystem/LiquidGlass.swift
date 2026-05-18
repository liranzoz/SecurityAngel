import SwiftUI

extension View {
    @ViewBuilder
    func liquidGlass<S: Shape>(in shape: S, tint: Color? = nil) -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular.tint(tint), in: shape)
        } else {
            self
                .background(.regularMaterial, in: shape)
                .overlay(shape.stroke(Color.white.opacity(0.18), lineWidth: 0.5))
        }
    }

    @ViewBuilder
    func liquidGlassCard(cornerRadius: CGFloat = 24, tint: Color? = nil) -> some View {
        self.liquidGlass(in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous), tint: tint)
    }

    @ViewBuilder
    func liquidGlassCapsule(tint: Color? = nil) -> some View {
        self.liquidGlass(in: Capsule(), tint: tint)
    }
}

struct LiquidGlassContainer<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer { content() }
        } else {
            content()
        }
    }
}
