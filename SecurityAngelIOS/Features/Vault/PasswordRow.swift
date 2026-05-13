import SwiftUI

struct PasswordRow: View {
    let account: MockPasswordAccount
    let isRevealed: Bool
    let onToggleReveal: () -> Void
    let onCopy: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            AsyncImage(url: faviconURL) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().aspectRatio(contentMode: .fit)
                default:
                    Image(systemName: "globe")
                        .font(.title2)
                        .foregroundStyle(Brand.primary)
                }
            }
            .frame(width: 44, height: 44)
            .liquidGlass(in: Circle())

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(account.siteName)
                        .font(.subheadline.weight(.semibold))
                    if account.isLeaked {
                        Image(systemName: "exclamationmark.shield.fill")
                            .font(.caption)
                            .foregroundStyle(Brand.unsafe)
                    }
                }
                Text(account.email)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(isRevealed ? account.password : "••••••••••")
                    .font(.system(.callout, design: .monospaced))
                    .foregroundStyle(isRevealed ? .primary : .secondary)
            }

            Spacer()

            HStack(spacing: 6) {
                Button(action: onToggleReveal) {
                    Image(systemName: isRevealed ? "eye.slash.fill" : "eye.fill")
                        .foregroundStyle(.primary)
                        .frame(width: 36, height: 36)
                        .liquidGlass(in: Circle())
                }
                Button(action: onCopy) {
                    Image(systemName: "doc.on.doc.fill")
                        .foregroundStyle(.primary)
                        .frame(width: 36, height: 36)
                        .liquidGlass(in: Circle())
                }
            }
        }
        .padding(12)
        .liquidGlassCard(cornerRadius: 18, tint: account.isLeaked ? Brand.unsafe.opacity(0.08) : nil)
    }

    private var faviconURL: URL? {
        URL(string: "https://www.google.com/s2/favicons?domain=\(account.domain)&sz=128")
    }
}

#Preview {
    VStack(spacing: 10) {
        ForEach(MockData.passwords) { acc in
            PasswordRow(account: acc, isRevealed: false, onToggleReveal: {}, onCopy: {})
        }
    }
    .padding()
    .background(Brand.backgroundGradient)
}
