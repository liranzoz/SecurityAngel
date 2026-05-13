import SwiftUI

struct PasswordGeneratorView: View {
    @State private var length: Double = 16
    @State private var includeUpper = true
    @State private var includeLower = true
    @State private var includeNumbers = true
    @State private var includeSymbols = true
    @State private var generated: String = ""
    @State private var copied: Bool = false

    private let upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private let lower = "abcdefghijklmnopqrstuvwxyz"
    private let numbers = "0123456789"
    private let symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    headerCard
                    optionsCard
                    PrimaryButton(title: "Generate", icon: "wand.and.stars") {
                        generate()
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
        }
        .navigationTitle("Password Generator")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if generated.isEmpty { generate() } }
    }

    private var headerCard: some View {
        GlassCard {
            VStack(spacing: 14) {
                Image(systemName: "wand.and.stars")
                    .font(.system(size: 36, weight: .semibold))
                    .foregroundStyle(Brand.primary)
                    .padding(16)
                    .liquidGlass(in: Circle(), tint: Brand.primary.opacity(0.15))

                Text(generated.isEmpty ? "—" : generated)
                    .font(.system(.title3, design: .monospaced, weight: .semibold))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                HStack(spacing: 12) {
                    Button {
                        UIPasteboard.general.string = generated
                        withAnimation { copied = true }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                            withAnimation { copied = false }
                        }
                    } label: {
                        HStack {
                            Image(systemName: copied ? "checkmark" : "doc.on.doc.fill")
                            Text(copied ? "Copied" : "Copy")
                        }
                        .font(.callout.bold())
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .foregroundStyle(.primary)
                        .liquidGlassCapsule()
                    }
                    Button { generate() } label: {
                        HStack {
                            Image(systemName: "arrow.triangle.2.circlepath")
                            Text("Reroll")
                        }
                        .font(.callout.bold())
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .foregroundStyle(.primary)
                        .liquidGlassCapsule()
                    }
                }
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal)
    }

    private var optionsCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Length")
                    Spacer()
                    Text("\(Int(length))")
                        .font(.title3.bold())
                        .foregroundStyle(Brand.primary)
                }
                Slider(value: $length, in: 8...64, step: 1)
                    .tint(Brand.primary)

                Divider().opacity(0.3)

                Toggle("Uppercase (A-Z)", isOn: $includeUpper).tint(Brand.primary)
                Toggle("Lowercase (a-z)", isOn: $includeLower).tint(Brand.primary)
                Toggle("Numbers (0-9)",   isOn: $includeNumbers).tint(Brand.primary)
                Toggle("Symbols (!@#$)",  isOn: $includeSymbols).tint(Brand.primary)
            }
        }
        .padding(.horizontal)
    }

    private func generate() {
        var pool = ""
        if includeUpper   { pool += upper }
        if includeLower   { pool += lower }
        if includeNumbers { pool += numbers }
        if includeSymbols { pool += symbols }
        guard !pool.isEmpty else { return }
        let n = Int(length)
        generated = String((0..<n).compactMap { _ in pool.randomElement() })
    }
}

#Preview {
    NavigationStack { PasswordGeneratorView() }
}
