import SwiftUI

struct AddMemberView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var generatedCode = ""
    @State private var showShare = false

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.backgroundGradient.ignoresSafeArea()
                VStack(spacing: 20) {
                    Image(systemName: "person.crop.circle.badge.plus")
                        .font(.system(size: 72))
                        .foregroundStyle(Brand.primary)
                        .padding(28)
                        .liquidGlass(in: Circle())
                        .padding(.top, 20)

                    Text("Invite a family member")
                        .font(Typography.title)
                    Text("Enter their email — we'll generate a secure 6-digit code they can use to join.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    GlassCard {
                        VStack(spacing: 14) {
                            GlassTextField(placeholder: "Email", text: $email, icon: "envelope.fill", keyboardType: .emailAddress, contentType: .emailAddress)
                            if !generatedCode.isEmpty {
                                HStack(spacing: 10) {
                                    ForEach(Array(generatedCode), id: \.self) { ch in
                                        Text(String(ch))
                                            .font(.system(.title, design: .rounded, weight: .bold))
                                            .frame(width: 42, height: 52)
                                            .liquidGlassCard(cornerRadius: 12)
                                    }
                                }
                            }
                            PrimaryButton(title: generatedCode.isEmpty ? "Generate Code" : "Share Invitation", icon: generatedCode.isEmpty ? "wand.and.stars" : "square.and.arrow.up") {
                                if generatedCode.isEmpty {
                                    generatedCode = String((0..<6).map { _ in "0123456789".randomElement()! })
                                } else {
                                    showShare = true
                                }
                            }
                            .disabled(email.isEmpty)
                            .opacity(email.isEmpty ? 0.5 : 1)
                        }
                    }
                    .padding(.horizontal)

                    Spacer()
                }
            }
            .navigationTitle("Add Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .sheet(isPresented: $showShare) {
                ShareLink(item: "Join my family on Security Angel. Use email \(email) and code \(generatedCode).") {
                    Label("Share Invitation", systemImage: "square.and.arrow.up")
                }
                .padding()
                .presentationDetents([.height(180)])
            }
        }
    }
}

#Preview {
    AddMemberView()
}
