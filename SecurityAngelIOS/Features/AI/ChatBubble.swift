import SwiftUI

struct ChatBubble: View {
    let message: ChatMessageDoc

    var body: some View {
        HStack {
            if message.isUser { Spacer(minLength: 40) }
            bubble
            if !message.isUser { Spacer(minLength: 40) }
        }
    }

    @ViewBuilder
    private var bubble: some View {
        if message.isLoading {
            HStack(spacing: 6) {
                ForEach(0..<3) { _ in
                    Circle()
                        .fill(Brand.primary.opacity(0.6))
                        .frame(width: 8, height: 8)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .liquidGlassCard(cornerRadius: 18)
        } else {
            VStack(alignment: message.isUser ? .trailing : .leading, spacing: 6) {
                if message.imageUri != nil {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(.gray.opacity(0.2))
                        .frame(height: 140)
                        .overlay(Image(systemName: "photo").font(.title).foregroundStyle(.secondary))
                }
                Text(LocalizedStringKey(message.text))
                    .font(.body)
                    .foregroundStyle(message.isUser ? .white : .primary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background {
                if message.isUser {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Brand.headerGradient)
                        .shadow(color: Brand.primary.opacity(0.3), radius: 8, y: 4)
                }
            }
            .liquidGlassCard(cornerRadius: 18)
        }
    }
}

#Preview {
    VStack(spacing: 8) {
        ChatBubble(message: ChatMessageDoc(text: "Hi there!", isUser: false))
        ChatBubble(message: ChatMessageDoc(text: "Check my vault.", isUser: true))
        ChatBubble(message: ChatMessageDoc(text: "", isUser: false, isLoading: true))
    }
    .padding()
    .background(Brand.backgroundGradient)
}
