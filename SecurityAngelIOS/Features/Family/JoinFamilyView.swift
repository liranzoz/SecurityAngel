import SwiftUI

struct JoinFamilyView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var code = ""
    @State private var error: String?

    var body: some View {
        NavigationStack {
            ZStack {
                Brand.backgroundGradient.ignoresSafeArea()
                VStack(spacing: 20) {
                    Image(systemName: "ticket.fill")
                        .font(.system(size: 64))
                        .foregroundStyle(Brand.primary)
                        .padding(24)
                        .liquidGlass(in: Circle())
                        .padding(.top, 16)

                    Text("Join a Family")
                        .font(Typography.title)
                    Text("Enter the 6-digit code you received.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    GlassCard {
                        VStack(spacing: 12) {
                            GlassTextField(placeholder: "6-digit code", text: $code, icon: "number", keyboardType: .numberPad)
                            if let error {
                                Text(error).font(.caption).foregroundStyle(.red)
                            }
                            HStack(spacing: 10) {
                                Button {
                                    if let str = UIPasteboard.general.string {
                                        code = str.filter(\.isNumber)
                                    }
                                } label: {
                                    HStack {
                                        Image(systemName: "doc.on.clipboard")
                                        Text("Paste")
                                    }
                                    .font(.callout.bold())
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .foregroundStyle(.primary)
                                    .liquidGlassCapsule()
                                }
                                PrimaryButton(title: "Join") {
                                    if code.count != 6 {
                                        error = "Please enter a valid 6-digit code"
                                    } else {
                                        error = nil
                                        dismiss()
                                    }
                                }
                            }
                        }
                    }
                    .padding(.horizontal)
                    Spacer()
                }
            }
            .navigationTitle("Join Family")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

#Preview {
    JoinFamilyView()
}
