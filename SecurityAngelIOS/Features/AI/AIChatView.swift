import SwiftUI

struct AIChatView: View {
    @Binding var showMenu: Bool
    @State private var messages: [MockChatMessage] = MockData.chatHistory
    @State private var draft: String = ""
    @State private var showImagePreview: Bool = false

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()

            VStack(spacing: 0) {
                header
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(messages) { message in
                                ChatBubble(message: message)
                                    .id(message.id)
                            }
                        }
                        .padding()
                    }
                    .onChange(of: messages.count) { _, _ in
                        withAnimation { proxy.scrollTo(messages.last?.id, anchor: .bottom) }
                    }
                }
                if showImagePreview { previewBar }
                inputBar
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { showMenu = true } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.title3.weight(.semibold))
                    .frame(width: 40, height: 40)
                    .liquidGlassCard(cornerRadius: 12)
            }
            Image(systemName: "sparkles")
                .foregroundStyle(Brand.primary)
            Text("Security Assistant")
                .font(Typography.sectionTitle)
            Spacer()
            Button {} label: {
                Image(systemName: "ellipsis.circle")
                    .font(.title3.weight(.semibold))
                    .frame(width: 40, height: 40)
                    .liquidGlassCard(cornerRadius: 12)
            }
        }
        .padding(.horizontal)
        .padding(.top, 8)
        .padding(.bottom, 4)
    }

    private var previewBar: some View {
        HStack {
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(.gray.opacity(0.2))
                    .frame(width: 80, height: 80)
                    .overlay(Image(systemName: "photo").foregroundStyle(.secondary))
                Button { showImagePreview = false } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.white, .black)
                }
                .offset(x: 8, y: -8)
            }
            Text("Photo attached for analysis")
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
        }
        .padding(.horizontal)
    }

    private var inputBar: some View {
        HStack(spacing: 10) {
            Button { showImagePreview = true } label: {
                Image(systemName: "photo.badge.plus")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Brand.primary)
                    .frame(width: 44, height: 44)
                    .liquidGlass(in: Circle())
            }
            HStack {
                TextField("Ask me anything…", text: $draft, axis: .vertical)
                    .lineLimit(1...4)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
            }
            .liquidGlassCapsule()
            Button {
                send()
            } label: {
                Image(systemName: "paperplane.fill")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(Brand.primary, in: Circle())
                    .shadow(color: Brand.primary.opacity(0.4), radius: 8, y: 4)
            }
            .disabled(draft.trimmingCharacters(in: .whitespaces).isEmpty)
            .opacity(draft.trimmingCharacters(in: .whitespaces).isEmpty ? 0.5 : 1)
        }
        .padding()
    }

    private func send() {
        let text = draft.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }
        messages.append(MockChatMessage(text: text, isUser: true, isLoading: false, hasImage: showImagePreview))
        draft = ""
        showImagePreview = false

        messages.append(MockChatMessage(text: "", isUser: false, isLoading: true, hasImage: false))
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            if let idx = messages.firstIndex(where: { $0.isLoading }) {
                messages.remove(at: idx)
            }
            messages.append(MockChatMessage(
                text: "I'm just a static placeholder for now — wiring Gemini next phase. ✨",
                isUser: false, isLoading: false, hasImage: false
            ))
        }
    }
}

#Preview {
    AIChatView(showMenu: .constant(false))
}
