import SwiftUI

enum PillKind {
    case safe, warning, unsafe, info

    var foreground: Color {
        switch self {
        case .safe:    return Brand.safe
        case .warning: return Brand.warning
        case .unsafe:  return Brand.unsafe
        case .info:    return Brand.iconBlue
        }
    }

    var background: Color {
        switch self {
        case .safe:    return Brand.safeBg
        case .warning: return Brand.warningBg
        case .unsafe:  return Brand.unsafeBg
        case .info:    return Brand.iconBlueBg
        }
    }

    var icon: String {
        switch self {
        case .safe:    return "checkmark.shield.fill"
        case .warning: return "exclamationmark.triangle.fill"
        case .unsafe:  return "xmark.shield.fill"
        case .info:    return "info.circle.fill"
        }
    }
}

struct StatusPill: View {
    let text: String
    let kind: PillKind
    var showIcon: Bool = true

    var body: some View {
        HStack(spacing: 6) {
            if showIcon {
                Image(systemName: kind.icon)
                    .font(.caption.weight(.bold))
            }
            Text(text)
                .font(.caption.weight(.bold))
        }
        .foregroundStyle(kind.foreground)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(kind.background, in: Capsule())
    }
}

#Preview {
    VStack(spacing: 12) {
        StatusPill(text: "Safe", kind: .safe)
        StatusPill(text: "2 Alerts", kind: .warning)
        StatusPill(text: "Compromised", kind: .unsafe)
        StatusPill(text: "Info", kind: .info)
    }
    .padding()
}
