import SwiftUI
import FirebaseFirestore

struct SecurityLogView: View {
    @Environment(AppState.self) private var appState

    @State private var logs: [SecurityLogDoc] = []
    @State private var listener: ListenerRegistration?
    @State private var showClearAlert = false
    @State private var accessDenied = false

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            if accessDenied {
                accessDeniedView
            } else if logs.isEmpty {
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
            if !accessDenied && appState.isAdmin {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(role: .destructive) {
                        showClearAlert = true
                    } label: { Image(systemName: "trash") }
                    .disabled(logs.isEmpty)
                }
            }
        }
        .alert("Clear Activity Log", isPresented: $showClearAlert) {
            Button("Delete", role: .destructive) { clear() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to delete all family activity history?")
        }
        .onAppear { attach() }
        .onDisappear { listener?.remove() }
    }

    private func attach() {
        guard appState.isAdmin, let familyId = appState.family?.id else {
            accessDenied = !appState.isAdmin && appState.family != nil
            return
        }
        listener?.remove()
        listener = appState.logger.observeFamilyLogs(familyId: familyId) { entries in
            Task { @MainActor in self.logs = entries }
        }
    }

    private func clear() {
        guard let familyId = appState.family?.id else { return }
        Task { try? await appState.logger.clearFamilyLogs(familyId: familyId) }
    }

    private func row(_ log: SecurityLogDoc) -> some View {
        let kind: PillKind = {
            switch log.typedEvent {
            case .leakFound, .malware: return .unsafe
            case .scanSafe:            return .safe
            case .memberAdded:         return .info
            default:                   return .info
            }
        }()
        let icon: String = {
            switch log.typedEvent {
            case .leakFound, .malware: return "exclamationmark.shield.fill"
            case .scanSafe:            return "checkmark.shield.fill"
            case .memberAdded:         return "person.crop.circle.badge.plus"
            default:                   return "info.circle.fill"
            }
        }()

        return HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(kind.foreground)
                .frame(width: 44, height: 44)
                .liquidGlass(in: Circle(), tint: kind.background)
            VStack(alignment: .leading, spacing: 2) {
                Text(title(for: log)).font(.subheadline.weight(.semibold))
                Text(log.description).font(.caption).foregroundStyle(.secondary)
                if !log.userName.isEmpty {
                    Text("by \(log.userName)").font(.caption2).foregroundStyle(.tertiary)
                }
            }
            Spacer()
            Text(log.date.shortStamp)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .liquidGlassCard(cornerRadius: 18)
    }

    private func title(for log: SecurityLogDoc) -> String {
        switch log.typedEvent {
        case .leakFound:    return "Security Breach Detected"
        case .scanSafe:     return "Scan Completed"
        case .malware:      return "Malicious URL"
        case .memberAdded:  return "Family Update"
        case .passGenerated: return "Password Generated"
        case .none:         return "System Event"
        }
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

    private var accessDeniedView: some View {
        VStack(spacing: 14) {
            Image(systemName: "lock.shield")
                .font(.system(size: 64))
                .foregroundStyle(Brand.primary)
            Text("Admin only")
                .font(Typography.title)
            Text("Activity logs are visible to family admins.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }
}

#Preview {
    NavigationStack { SecurityLogView() }
        .environment(AppState())
}
