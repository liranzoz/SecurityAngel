import SwiftUI

struct DevicePostureView: View {
    @State private var posture: DevicePosture = DevicePostureService.evaluate()
    @State private var isRefreshing: Bool = false

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    summaryCard
                    SectionHeader("Device Checks").padding(.horizontal, 24)
                    VStack(spacing: 10) {
                        checkRow(
                            "Jailbreak Detection",
                            isSafe: !posture.jailbreak.isJailbroken,
                            detail: posture.jailbreak.isJailbroken
                                ? jailbreakReason
                                : "No jailbreak signals detected",
                            icon: "lock.iphone"
                        )
                        checkRow(
                            "Passcode Set",
                            isSafe: posture.passcodeSet,
                            detail: posture.passcodeSet ? "Device passcode is enabled" : "No passcode set",
                            icon: "key.fill"
                        )
                        checkRow(
                            biometricTitle,
                            isSafe: posture.biometricsEnrolled,
                            detail: posture.biometricsEnrolled ? "Enrolled and active" : "Not configured",
                            icon: biometricIcon
                        )
                        checkRow(
                            "Sandbox Integrity",
                            isSafe: !posture.jailbreak.canEscapeSandbox,
                            detail: posture.jailbreak.canEscapeSandbox
                                ? "Sandbox escape possible — device is compromised"
                                : "App sandbox is intact",
                            icon: "shippingbox.fill"
                        )
                        checkRow(
                            "Lockdown Mode",
                            isSafe: !posture.isOnLockdownMode,
                            detail: posture.isOnLockdownMode
                                ? "Lockdown Mode active — some features restricted"
                                : "Standard mode",
                            icon: "shield.lefthalf.filled"
                        )
                    }.padding(.horizontal)

                    SectionHeader("Notes")
                        .padding(.horizontal, 24)
                        .padding(.top, 8)
                    Text("On iOS, third-party apps' permissions are sandboxed and cannot be enumerated by another app. This view shows device-level signals instead — they are the closest analogue to Android's Permission Monitor.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal)

                    Spacer(minLength: 60)
                }
                .padding(.top, 8)
            }
        }
        .navigationTitle("Device Posture")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    refresh()
                } label: { Image(systemName: isRefreshing ? "arrow.clockwise.circle.fill" : "arrow.clockwise") }
                .disabled(isRefreshing)
            }
        }
    }

    private var summaryCard: some View {
        GlassCard {
            HStack(spacing: 12) {
                Image(systemName: posture.isHealthy ? "checkmark.shield.fill" : "exclamationmark.shield.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(posture.isHealthy ? Brand.safe : Brand.unsafe)
                    .padding(12)
                    .liquidGlass(in: Circle(), tint: (posture.isHealthy ? Brand.safe : Brand.unsafe).opacity(0.15))
                VStack(alignment: .leading) {
                    Text(posture.isHealthy ? "Device Healthy" : "Device At Risk")
                        .font(Typography.title)
                    Text("Last check: \(posture.lastEvaluated.relativeString)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
        .padding(.horizontal)
    }

    private func checkRow(_ title: String, isSafe: Bool, detail: String, icon: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Brand.primary)
                .frame(width: 40, height: 40)
                .liquidGlass(in: Circle())
            VStack(alignment: .leading) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(detail).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            StatusPill(text: isSafe ? "OK" : "Risk", kind: isSafe ? .safe : .unsafe, showIcon: false)
        }
        .padding(12)
        .liquidGlassCard(cornerRadius: 16)
    }

    private var jailbreakReason: String {
        var parts: [String] = []
        if posture.jailbreak.suspiciousFilesFound { parts.append("suspicious files") }
        if posture.jailbreak.canEscapeSandbox     { parts.append("sandbox writable") }
        if posture.jailbreak.canFork              { parts.append("fork() succeeds") }
        if posture.jailbreak.cydiaURLOpenable     { parts.append("cydia:// scheme") }
        return parts.isEmpty ? "Heuristic match" : parts.joined(separator: ", ")
    }

    private var biometricTitle: String {
        switch posture.biometricKind {
        case .faceID:  return "Face ID"
        case .touchID: return "Touch ID"
        case .opticID: return "Optic ID"
        case .none:    return "Biometrics"
        }
    }

    private var biometricIcon: String {
        switch posture.biometricKind {
        case .faceID:  return "faceid"
        case .touchID: return "touchid"
        case .opticID: return "opticid"
        case .none:    return "lock"
        }
    }

    private func refresh() {
        isRefreshing = true
        Task {
            try? await Task.sleep(for: .milliseconds(400))
            await MainActor.run {
                posture = DevicePostureService.evaluate()
                isRefreshing = false
            }
        }
    }
}

#Preview {
    NavigationStack { DevicePostureView() }
}
