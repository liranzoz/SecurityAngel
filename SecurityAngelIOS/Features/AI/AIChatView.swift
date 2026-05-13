import SwiftUI
import PhotosUI

struct AIChatView: View {
    @Binding var showMenu: Bool
    @Environment(AppState.self) private var appState

    @State private var messages: [ChatMessageDoc] = []
    @State private var draft: String = ""
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var selectedImage: UIImage? = nil
    @State private var pendingTask: Task<Void, Never>? = nil
    @State private var pendingAction: String? = nil
    @State private var didLoad = false

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
        .onAppear { loadHistory() }
        .onDisappear { pendingTask?.cancel() }
        .alert(actionAlertTitle, isPresented: Binding(get: { pendingAction != nil }, set: { if !$0 { pendingAction = nil } })) {
            Button("Take me", role: .none) { pendingAction = nil /* deep-link wiring next phase */ }
            Button("Stay here", role: .cancel) { pendingAction = nil }
        }
    }

    private var actionAlertTitle: String {
        switch pendingAction {
        case "OPEN_VAULT":    return "Open the password vault?"
        case "OPEN_FAMILY":   return "Open Family Safety?"
        case "OPEN_SCANNER":  return "Open the URL scanner?"
        case "GENERATE_PASS": return "Open the password generator?"
        case "OPEN_LOGS":     return "Open the activity log?"
        default:              return "Take action?"
        }
    }

    // MARK: - Header

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
            .disabled(!canSend)
            .opacity(canSend ? 1 : 0.5)
        }
        .padding()
    }

    private var canSend: Bool {
        !draft.trimmingCharacters(in: .whitespaces).isEmpty || selectedImage != nil
    }

    // MARK: - History

    private func loadHistory() {
        guard !didLoad, let uid = appState.firebaseUserId else { return }
        didLoad = true
        Task {
            let stored = (try? await appState.chatRepo.load(uid: uid)) ?? []
            await MainActor.run {
                if stored.isEmpty {
                    messages = [ChatMessageDoc(text: "Hello! I'm Security Angel AI. How can I help you stay safe today?", isUser: false)]
                } else {
                    messages = stored.filter { !$0.isLoading }
                }
            }
        }
    }

    // MARK: - Send

    private func send() {
        let text = draft.trimmingCharacters(in: .whitespaces)
        let image = selectedImage
        guard !text.isEmpty || image != nil else { return }
        guard let uid = appState.firebaseUserId else { return }

        let userMsg = ChatMessageDoc(text: text, isUser: true, imageUri: image == nil ? nil : "<inline>")
        messages.append(userMsg)
        Task { try? await appState.chatRepo.add(uid: uid, message: userMsg) }

        draft = ""
        selectedImage = nil
        selectedPhoto = nil

        let loadingId = UUID().uuidString
        messages.append(ChatMessageDoc(id: loadingId, isUser: false, isLoading: true))

        pendingTask = Task {
            do {
                let prompt = await buildPrompt(userText: text)
                let reply = try await gemini.generate(prompt: prompt, image: image)
                await applyReply(reply, removingLoadingId: loadingId, uid: uid)
            } catch {
                await applyReply("⚠️ \(error.localizedDescription)", removingLoadingId: loadingId, uid: uid)
            }
        }
    }

    private func buildPrompt(userText: String) async -> String {
        let user = appState.currentUser
        let familyStatus: String = {
            if appState.family == nil { return "Not in a family yet." }
            return appState.familyAlertCount == 0 ? "Safe" : "At Risk (\(appState.familyAlertCount) alerts)"
        }()
        let lastScan: String = {
            guard let scan = appState.recentScans.first else { return "No scans yet" }
            return "\(scan.url) - \(scan.isSafe ? "Safe" : "Malicious")"
        }()
        let postureSummary: String = {
            let p = appState.devicePosture
            if p.jailbreak.isJailbroken { return "JAILBROKEN — device appears to be compromised" }
            return "Healthy (passcode \(p.passcodeSet ? "on" : "off"), biometrics \(p.biometricsEnrolled ? "on" : "off"))"
        }()

        return GeminiPromptBuilder.buildPrompt(
            userName: user?.firstName ?? "user",
            totalPasswords: appState.totalPasswordCount,
            leakedPasswords: appState.leakedPasswordCount,
            familyStatus: familyStatus,
            lastScan: lastScan,
            rootStatus: postureSummary,
            riskyAppsText: "Not enumerable on iOS",
            userMessage: userText
        )
    }

    @MainActor
    private func applyReply(_ raw: String, removingLoadingId: String, uid: String) {
        let (cleaned, action) = GeminiPromptBuilder.extractAction(from: raw)
        messages.removeAll { $0.id == removingLoadingId }
        let botMsg = ChatMessageDoc(text: cleaned, isUser: false)
        messages.append(botMsg)
        Task { try? await appState.chatRepo.add(uid: uid, message: botMsg) }
        if let action {
            // Surface a confirmation; navigation deep-link wiring lives in
            // RootView next phase (needs tab + sheet bindings).
            pendingAction = action
        }
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
        .environment(AppState())
}
