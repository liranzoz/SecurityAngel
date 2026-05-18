import SwiftUI

struct GlassCard<Content: View>: View {
    var padding: CGFloat = 20
    var cornerRadius: CGFloat = 24
    var tint: Color? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .liquidGlassCard(cornerRadius: cornerRadius, tint: tint)
    }
}

struct TileCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: .infinity, minHeight: 150, alignment: .leading)
            .padding(20)
            .liquidGlassCard(cornerRadius: 28)
    }
}

#Preview {
    VStack(spacing: 16) {
        GlassCard {
            Text("Hello, glass")
                .font(Typography.title)
        }
        HStack(spacing: 12) {
            TileCard {
                VStack(alignment: .leading) {
                    Image(systemName: "lock.shield.fill")
                        .font(.title)
                        .foregroundStyle(Brand.primary)
                    Spacer()
                    Text("My Vault").font(Typography.sectionTitle)
                    Text("Manage Passwords").font(Typography.caption).foregroundStyle(.secondary)
                }
            }
            TileCard {
                VStack(alignment: .leading) {
                    Image(systemName: "person.3.fill")
                        .font(.title)
                        .foregroundStyle(Brand.primary)
                    Spacer()
                    Text("Family Safety").font(Typography.sectionTitle)
                    Text("Safe").font(Typography.caption).foregroundStyle(Brand.safe)
                }
            }
        }
    }
    .padding()
    .background(Brand.backgroundGradient)
}
