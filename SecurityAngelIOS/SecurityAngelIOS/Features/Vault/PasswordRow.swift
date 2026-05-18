import SwiftUI

/// Renders a single decrypted vault entry. Decryption happens inline on
/// each render — cheap because `VaultCryptoManager` caches the derived key.
struct VaultEntryRow: View {
    let entry: VaultEntry
    let pin: String
    let salt: String
    let isRevealed: Bool
    let onToggleReveal: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State private var copiedFlag = false

    var body: some View {
        let decryptedEmail    = VaultCryptoManager.decrypt(base64Ciphertext: entry.email,    pin: pin, saltBase64: salt)
        let decryptedPassword = VaultCryptoManager.decrypt(base64Ciphertext: entry.password, pin: pin, saltBase64: salt)

        return HStack(spacing: 14) {
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
                    Text(entry.siteName)
                        .font(.subheadline.weight(.semibold))
                    if entry.isLeaked {
                        Image(systemName: "exclamationmark.shield.fill")
                            .font(.caption)
                            .foregroundStyle(Brand.unsafe)
                    }
                }
                Text(decryptedEmail.isEmpty ? "—" : decryptedEmail)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(isRevealed ? (decryptedPassword.isEmpty ? "—" : decryptedPassword) : "••••••••••")
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
                Button {
                    UIPasteboard.general.string = decryptedPassword
                    withAnimation { copiedFlag = true }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                        withAnimation { copiedFlag = false }
                    }
                } label: {
                    Image(systemName: copiedFlag ? "checkmark" : "doc.on.doc.fill")
                        .foregroundStyle(.primary)
                        .frame(width: 36, height: 36)
                        .liquidGlass(in: Circle())
                }
            }
        }
        .padding(12)
        .liquidGlassCard(cornerRadius: 18, tint: entry.isLeaked ? Brand.unsafe.opacity(0.08) : nil)
        .contextMenu {
            Button("Edit", systemImage: "pencil") { onEdit() }
            Button("Delete", systemImage: "trash", role: .destructive) { onDelete() }
        }
    }

    private var faviconURL: URL? {
        URL(string: "https://www.google.com/s2/favicons?domain=\(entry.domain)&sz=128")
    }
}
