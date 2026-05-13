import SwiftUI

enum ScanState {
    case idle
    case scanning(String)
    case safe(maliciousCount: Int)
    case unsafe(maliciousCount: Int)
}

struct URLScannerView: View {
    @Binding var showMenu: Bool
    @State private var url: String = ""
    @State private var state: ScanState = .idle
    @State private var showInfo: Bool = false
    private let results = MockData.engineResults

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    ScreenTitleBar(
                        title: "URL Scanner",
                        onMenu: { showMenu = true },
                        trailing: AnyView(
                            Button { showInfo = true } label: {
                                Image(systemName: "info.circle")
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(.primary)
                                    .frame(width: 40, height: 40)
                                    .liquidGlassCard(cornerRadius: 12)
                            }
                        )
                    )
                    .padding(.top, 8)

                    inputCard
                    resultCard
                    if shouldShowEngines { enginesCard }
                    Spacer(minLength: 80)
                }
                .padding(.bottom)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showInfo) {
            scannerInfoSheet
                .presentationDetents([.medium])
                .presentationBackground(.regularMaterial)
        }
    }

    private var inputCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 14) {
                Text("Check a URL")
                    .font(Typography.sectionTitle)
                HStack(spacing: 10) {
                    GlassTextField(placeholder: "https://example.com", text: $url, icon: "link", keyboardType: .URL, contentType: .URL)
                    Button {
                        if let str = UIPasteboard.general.string { url = str }
                    } label: {
                        Image(systemName: "doc.on.clipboard")
                            .foregroundStyle(.primary)
                            .frame(width: 48, height: 48)
                            .liquidGlassCard(cornerRadius: 16)
                    }
                }
                PrimaryButton(
                    title: scanButtonTitle,
                    icon: "magnifyingglass",
                    isLoading: isScanning
                ) {
                    runMockScan()
                }
                .disabled(url.isEmpty || isScanning)
                .opacity(url.isEmpty ? 0.5 : 1)
            }
        }
        .padding(.horizontal)
    }

    private var resultCard: some View {
        GlassCard {
            VStack(alignment: .center, spacing: 12) {
                Image(systemName: resultIcon)
                    .font(.system(size: 56, weight: .semibold))
                    .foregroundStyle(resultColor)
                    .padding(20)
                    .liquidGlass(in: Circle(), tint: resultColor.opacity(0.15))

                Text(resultTitle)
                    .font(Typography.title)
                Text(resultSubtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
        }
        .padding(.horizontal)
    }

    private var enginesCard: some View {
        VStack(spacing: 12) {
            SectionHeader("Engine Results")
                .padding(.horizontal, 24)
            VStack(spacing: 6) {
                ForEach(results) { r in
                    HStack {
                        Image(systemName: r.isClean ? "checkmark.shield.fill" : "exclamationmark.triangle.fill")
                            .foregroundStyle(r.isClean ? Brand.safe : Brand.unsafe)
                        Text(r.name)
                        Spacer()
                        Text(r.status.capitalized)
                            .font(.caption.bold())
                            .foregroundStyle(r.isClean ? Brand.safe : Brand.unsafe)
                    }
                    .padding(.vertical, 12)
                    .padding(.horizontal, 16)
                    .liquidGlassCard(cornerRadius: 16)
                }
            }
            .padding(.horizontal)
        }
    }

    private var scannerInfoSheet: some View {
        VStack(alignment: .leading, spacing: 16) {
            Capsule().fill(.gray.opacity(0.3)).frame(width: 40, height: 4).frame(maxWidth: .infinity)
            Text("How it works").font(Typography.title)
            Text("Our scanner analyzes URLs using over 70 antivirus engines to detect malware, phishing, and other threats.")
                .font(.body)
            Label("Green check — the site is safe.", systemImage: "checkmark.shield.fill")
                .foregroundStyle(Brand.safe)
            Label("Red warning — threats were detected.", systemImage: "exclamationmark.triangle.fill")
                .foregroundStyle(Brand.unsafe)
            Text("⚠️ While we use advanced analysis tools, no system is 100% perfect. Accessing any website is strictly at your own risk.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
    }

    // MARK: helpers

    private var isScanning: Bool {
        if case .scanning = state { return true } else { return false }
    }

    private var scanButtonTitle: String {
        if case .scanning(let label) = state { return label }
        return "Scan"
    }

    private var shouldShowEngines: Bool {
        switch state {
        case .safe, .unsafe: return true
        default: return false
        }
    }

    private var resultIcon: String {
        switch state {
        case .idle:     return "shield.lefthalf.filled"
        case .scanning: return "hourglass"
        case .safe:     return "checkmark.shield.fill"
        case .unsafe:   return "exclamationmark.triangle.fill"
        }
    }

    private var resultColor: Color {
        switch state {
        case .idle, .scanning: return Brand.primary
        case .safe:            return Brand.safe
        case .unsafe:          return Brand.unsafe
        }
    }

    private var resultTitle: String {
        switch state {
        case .idle:     return "Ready"
        case .scanning(let s): return s
        case .safe:     return "Safe"
        case .unsafe(let n): return "Unsafe!"
        }
    }

    private var resultSubtitle: String {
        switch state {
        case .idle:     return "Paste a URL and tap Scan."
        case .scanning: return "Querying VirusTotal…"
        case .safe:     return "No threats detected by any of 70+ engines."
        case .unsafe(let n): return "Found \(n) threats."
        }
    }

    private func runMockScan() {
        state = .scanning("Checking Database…")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
            state = .scanning("Analyzing with 70 engines…")
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                let isSafe = url.contains("github") || url.contains("apple") || url.contains("news")
                state = isSafe ? .safe(maliciousCount: 0) : .unsafe(maliciousCount: 3)
            }
        }
    }
}

#Preview {
    URLScannerView(showMenu: .constant(false))
}
