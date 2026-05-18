import SwiftUI

struct SectionHeader: View {
    let title: String
    var trailing: AnyView? = nil

    init(_ title: String, trailing: AnyView? = nil) {
        self.title = title
        self.trailing = trailing
    }

    var body: some View {
        HStack {
            Text(title).font(Typography.sectionTitle)
            Spacer()
            if let trailing { trailing }
        }
        .padding(.horizontal, 4)
    }
}

struct ScreenTitleBar: View {
    let title: String
    var onMenu: (() -> Void)? = nil
    var trailing: AnyView? = nil

    var body: some View {
        HStack {
            if let onMenu {
                Button(action: onMenu) {
                    Image(systemName: "line.3.horizontal")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(.primary)
                        .frame(width: 40, height: 40)
                        .liquidGlassCard(cornerRadius: 12)
                }
            }
            Spacer()
            Text(title)
                .font(Typography.title)
            Spacer()
            if let trailing {
                trailing
            } else if onMenu != nil {
                Color.clear.frame(width: 40, height: 40)
            }
        }
        .padding(.horizontal)
    }
}

#Preview {
    VStack(spacing: 16) {
        ScreenTitleBar(title: "Dashboard", onMenu: {})
        SectionHeader("Recent Scans")
    }
    .padding()
    .background(Brand.backgroundGradient)
}
