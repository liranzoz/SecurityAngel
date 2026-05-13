import SwiftUI

struct DevicePostureView: View {
    @State private var lastScan: Date = .now
    @State private var jailbroken: Bool = false
    @State private var passcodeSet: Bool = true
    @State private var biometricsEnabled: Bool = true
    @State private var iCloudKeychainOn: Bool = true
    @State private var screenLockTimeout: Int = 60

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    summaryCard
                    SectionHeader("Device Checks").padding(.horizontal, 24)
                    VStack(spacing: 10) {
                        checkRow("Jailbreak Detection", isSafe: !jailbroken, detail: jailbroken ? "Suspicious files detected" : "No jailbreak detected", icon: "lock.iphone")
                        checkRow("Passcode Set", isSafe: passcodeSet, detail: passcodeSet ? "Device passcode is enabled" : "No passcode set", icon: "key.fill")
                        checkRow("Biometrics", isSafe: biometricsEnabled, detail: biometricsEnabled ? "Face ID enabled" : "Not configured", icon: "faceid")
                        checkRow("iCloud Keychain", isSafe: iCloudKeychainOn, detail: iCloudKeychainOn ? "Synced & enabled" : "Disabled", icon: "icloud.fill")
                        checkRow("Auto-Lock", isSafe: screenLockTimeout <= 120, detail: "Locks after \(screenLockTimeout) seconds", icon: "moon.fill")
                    }.padding(.horizontal)

                    SectionHeader("Risky Apps")
                        .padding(.horizontal, 24)
                        .padding(.top, 8)
                    Text("On iOS, third-party apps' permissions are not enumerable. This list shows apps you've granted system-level capabilities to.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal)
                    VStack(spacing: 10) {
                        ForEach(MockData.riskyApps) { app in
                            riskyAppRow(app)
                        }
                    }.padding(.horizontal)

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
                    withAnimation { lastScan = .now }
                } label: { Image(systemName: "arrow.clockwise") }
            }
        }
    }

    private var summaryCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: jailbroken ? "exclamationmark.shield.fill" : "checkmark.shield.fill")
                        .font(.system(size: 32))
                        .foregroundStyle(jailbroken ? Brand.unsafe : Brand.safe)
                        .padding(12)
                        .liquidGlass(in: Circle(), tint: (jailbroken ? Brand.unsafe : Brand.safe).opacity(0.15))
                    VStack(alignment: .leading) {
                        Text(jailbroken ? "Device At Risk" : "Device Healthy")
                            .font(Typography.title)
                        Text("Last scan: \(lastScan.relativeString)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                }
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

    private func riskyAppRow(_ app: MockRiskyApp) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "app.fill")
                .font(.title3)
                .foregroundStyle(Brand.primary)
                .frame(width: 40, height: 40)
                .liquidGlass(in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(app.appName).font(.subheadline.weight(.semibold))
                Text(app.permissions).font(.caption2).foregroundStyle(.secondary)
            }
            Spacer()
            StatusPill(text: app.riskLabel, kind: .forRiskLabel(app.riskLabel))
        }
        .padding(12)
        .liquidGlassCard(cornerRadius: 16)
    }
}

#Preview {
    NavigationStack { DevicePostureView() }
}
