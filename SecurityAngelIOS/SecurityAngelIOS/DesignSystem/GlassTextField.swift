import SwiftUI

struct GlassTextField: View {
    let placeholder: String
    @Binding var text: String
    var icon: String? = nil
    var isSecure: Bool = false
    var keyboardType: UIKeyboardType = .default
    var contentType: UITextContentType? = nil

    var body: some View {
        HStack(spacing: 12) {
            if let icon {
                Image(systemName: icon)
                    .foregroundStyle(Brand.primary)
                    .frame(width: 22)
            }
            Group {
                if isSecure {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text)
                }
            }
            .keyboardType(keyboardType)
            .textContentType(contentType)
            .autocorrectionDisabled(isSecure)
            .textInputAutocapitalization(isSecure ? .never : .sentences)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .liquidGlassCard(cornerRadius: 16)
    }
}

struct GlassSearchField: View {
    let placeholder: String
    @Binding var text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField(placeholder, text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .liquidGlassCapsule()
    }
}

#Preview {
    @Previewable @State var email = ""
    @Previewable @State var pin = ""
    @Previewable @State var query = ""
    return VStack(spacing: 16) {
        GlassTextField(placeholder: "Email", text: $email, icon: "envelope.fill", keyboardType: .emailAddress, contentType: .emailAddress)
        GlassTextField(placeholder: "Password", text: $pin, icon: "lock.fill", isSecure: true)
        GlassSearchField(placeholder: "Search accounts…", text: $query)
    }
    .padding()
    .background(Brand.backgroundGradient)
}
