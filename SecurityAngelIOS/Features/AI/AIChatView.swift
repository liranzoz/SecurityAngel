import SwiftUI
import PhotosUI

struct AIChatView: View {
    @Binding var showMenu: Bool
    @State private var messages: [MockChatMessage] = MockData.chatHistory
    @State private var draft: String = ""
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var selectedImage: UIImage? = nil
    @State private var pendingTask: Task<Void, Never>? = nil

    private let gemini = GeminiAPI()

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
                if selectedImage != nil { previewBar }
                inputBar
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onChange(of: selectedPhoto) { _, newItem in loadPhoto(newItem) }
        .onDisappear { pendingTask?.cancel() }
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
            if let img = selectedImage {
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 80, height: 80)
                    .clipShape(.rect(cornerRadius: 12))
                    .overlay(alignment: .topTrailing) {
                        Button {
                            selectedImage = nil
                            selectedPhoto = nil
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.white, .black)
                        }
                        .offset(x: 8, y: -8)
                    }
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
            PhotosPicker(selection: $selectedPhoto, matching: .images, photoLibrary: .shared()) {
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
            .disabled(draft.trimmingCharacters(in: .whitespaces).isEmpty && selectedImage == nil)
            .opacity(canSend ? 1 : 0.5)
        }
        .padding()
    }

    private var canSend: Bool {
        !draft.trimmingCharacters(in: .whitespaces).isEmpty || selectedImage != nil
    }

    // MARK: - Send

    private func send() {
        let text = draft.trimmingCharacters(in: .whitespaces)
        let image = selectedImage
        guard !text.isEmpty || image != nil else { return }

        messages.append(MockChatMessage(text: text, isUser: true, isLoading: false, hasImage: image != nil))
        draft = ""
        selectedImage = nil
        selectedPhoto = nil

        messages.append(MockChatMessage(text: "", isUser: false, isLoading: true, hasImage: false))

        pendingTask = Task {
            do {
                // No real Firestore context yet — pass placeholders this turn.
                let prompt = GeminiPromptBuilder.buildPrompt(
                    userName: MockData.currentUser.firstName,
                    totalPasswords: MockData.passwords.count,
                    leakedPasswords: MockData.passwords.filter(\.isLeaked).count,
                    familyStatus: "Safe",
                    lastScan: "No recent scans",
                    rootStatus: JailbreakDetector.evaluate().isJailbroken ? "JAILBROKEN" : "Healthy",
                    riskyAppsText: "None enumerable on iOS",
                    userMessage: text
                )
                let reply = try await gemini.generate(prompt: prompt, image: image)
                await applyReply(reply)
            } catch {
                await applyReply("⚠️ \(error.localizedDescription)")
            }
        }
    }

    @MainActor
    private func applyReply(_ raw: String) {
        let (text, action) = GeminiPromptBuilder.extractAction(from: raw)
        if let idx = messages.firstIndex(where: { $0.isLoading }) {
            messages.remove(at: idx)
        }
        messages.append(MockChatMessage(text: text, isUser: false, isLoading: false, hasImage: false))
        _ = action  // wiring deep-link actions comes with AppState next turn
    }

    private func loadPhoto(_ item: PhotosPickerItem?) {
        guard let item else { return }
        Task {
            if let data = try? await item.loadTransferable(type: Data.self),
               let img = UIImage(data: data) {
                await MainActor.run { selectedImage = img }
            }
        }
    }
}

#Preview {
    AIChatView(showMenu: .constant(false))
}
