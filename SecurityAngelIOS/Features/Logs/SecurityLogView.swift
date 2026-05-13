import SwiftUI

struct SecurityLogView: View {
    @State private var logs: [MockSecurityLog] = MockData.logs
    @State private var showClearAlert = false

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            if logs.isEmpty {
                emptyState
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(logs) { log in
                            row(log)
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("Activity Log")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    showClearAlert = true
                } label: { Image(systemName: "trash") }
                .disabled(logs.isEmpty)
            }
        }
        .alert("Clear Activity Log", isPresented: $showClearAlert) {
            Button("Delete", role: .destructive) { logs.removeAll() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to delete all history?")
        }
    }

    private func row(_ log: MockSecurityLog) -> some View {
        HStack(spacing: 14) {
            Image(systemName: log.icon)
                .font(.title3)
                .foregroundStyle(log.kind.foreground)
                .frame(width: 44, height: 44)
                .liquidGlass(in: Circle(), tint: log.kind.background)
            VStack(alignment: .leading, spacing: 2) {
                Text(log.title).font(.subheadline.weight(.semibold))
                Text(log.description).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text(log.timestamp.shortStamp)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .liquidGlassCard(cornerRadius: 18)
    }

    private var emptyState: some View {
        VStack(spacing: 14) {
            Image(systemName: "tray")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("No activity yet").font(Typography.title)
            Text("Family security events will appear here.")
                .foregroundStyle(.secondary)
        }
    }
}

#Preview {
    NavigationStack { SecurityLogView() }
}
