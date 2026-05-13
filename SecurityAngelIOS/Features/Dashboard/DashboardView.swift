import SwiftUI

struct DashboardView: View {
    @Binding var showMenu: Bool
    var onSelectTab: (AppTab) -> Void = { _ in }
    @Environment(AppState.self) private var appState

    private var user: SecurityUser? { appState.currentUser }
    private var score: Int { appState.dashboardScore.finalScore }
    private var familyAlertCount: Int { appState.familyAlertCount }
    private var recentScans: [ScanHistoryItem] { appState.recentScans }

    var body: some View {
        ZStack {
            Brand.backgroundGradient.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    header
                    tilesRow
                    if !recentScans.isEmpty {
                        recentScansSection
                    } else {
                        emptyScansSection
                    }
                    Spacer(minLength: 80)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            appState.devicePosture = DevicePostureService.evaluate()
        }
    }

    private var header: some View {
        VStack(spacing: 12) {
            HStack {
                Button { showMenu = true } label: {
                    Image(systemName: "line.3.horizontal")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                        .liquidGlass(in: Circle())
                }
                Spacer()
                VStack(spacing: 2) {
                    Text("Welcome back")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.85))
                    Text(user?.firstName ?? "")
                        .font(.headline)
                        .foregroundStyle(.white)
                }
                Spacer()
                Image(systemName: "bell.fill")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .liquidGlass(in: Circle())
            }
            .padding(.horizontal)

            ScoreRing(score: score, size: 220)
        }
        .padding(.top, 4)
        .padding(.bottom, 56)
        .frame(maxWidth: .infinity)
        .background {
            Brand.headerGradient
                .overlay(alignment: .topLeading) {
                    Circle()
                        .fill(.white.opacity(0.08))
                        .frame(width: 200, height: 200)
                        .offset(x: -50, y: -40)
                }
                .overlay(alignment: .topTrailing) {
                    Circle()
                        .fill(.white.opacity(0.06))
                        .frame(width: 140, height: 140)
                        .offset(x: 40, y: 100)
                }
                .clipShape(.rect(bottomLeadingRadius: 36, bottomTrailingRadius: 36))
                .ignoresSafeArea(edges: .top)
        }
    }

    private var tilesRow: some View {
        HStack(spacing: 14) {
            Button { onSelectTab(.vault) } label: {
                TileCard {
                    VStack(alignment: .leading, spacing: 8) {
                        LottieAnimation(animation: .vault, speed: 1.0)
                            .frame(width: 56, height: 56)
                        Spacer()
                        Text("My Vault").font(Typography.sectionTitle)
                        Text(vaultSubtitle).font(.caption).foregroundStyle(vaultSubtitleColor)
                    }
                }
            }
            .buttonStyle(.plain)

            Button { onSelectTab(.family) } label: {
                TileCard {
                    VStack(alignment: .leading, spacing: 8) {
                        LottieAnimation(animation: .familyBold, speed: appState.familyAlertCount > 0 ? 1.2 : 0.5)
                            .frame(width: 56, height: 56)
                        Spacer()
                        Text("Family Safety").font(Typography.sectionTitle)
                        Text(familySubtitle).font(.caption).foregroundStyle(familySubtitleColor)
                    }
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal)
        .offset(y: -50)
        .padding(.bottom, -50)
    }

    private var vaultSubtitle: String {
        if appState.totalPasswordCount == 0 { return "Add your first password" }
        let leaks = appState.leakedPasswordCount
        return leaks == 0
            ? "\(appState.totalPasswordCount) safe"
            : "\(leaks) compromised"
    }

    private var vaultSubtitleColor: Color {
        appState.leakedPasswordCount > 0 ? Brand.unsafe : .secondary
    }

    private var familySubtitle: String {
        if appState.family == nil { return "Not in a family yet" }
        return familyAlertCount == 0 ? "All safe" : "\(familyAlertCount) alert\(familyAlertCount == 1 ? "" : "s")"
    }

    private var familySubtitleColor: Color {
        familyAlertCount > 0 ? Brand.unsafe : .secondary
    }

    private var recentScansSection: some View {
        VStack(spacing: 12) {
            SectionHeader("Recent Scans")
                .padding(.horizontal)
            VStack(spacing: 10) {
                ForEach(recentScans) { scan in
                    HStack(spacing: 12) {
                        Image(systemName: scan.isSafe ? "checkmark.shield.fill" : "exclamationmark.triangle.fill")
                            .font(.title3)
                            .foregroundStyle(scan.isSafe ? Brand.safe : Brand.unsafe)
                            .frame(width: 40, height: 40)
                            .liquidGlass(in: Circle(), tint: (scan.isSafe ? Brand.safe : Brand.unsafe).opacity(0.12))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(scan.url)
                                .font(.subheadline.weight(.semibold))
                                .lineLimit(1)
                                .truncationMode(.middle)
                            Text(scan.date.relativeString)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        StatusPill(text: scan.isSafe ? "Safe" : "Unsafe", kind: scan.isSafe ? .safe : .unsafe)
                    }
                    .padding(14)
                    .liquidGlassCard(cornerRadius: 18)
                }
            }
            .padding(.horizontal)
        }
        .padding(.top, 24)
    }

    private var emptyScansSection: some View {
        VStack(spacing: 10) {
            SectionHeader("Recent Scans").padding(.horizontal)
            GlassCard {
                HStack(spacing: 12) {
                    Image(systemName: "magnifyingglass.circle.fill")
                        .font(.title2)
                        .foregroundStyle(Brand.primary)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("No scans yet").font(.subheadline.weight(.semibold))
                        Text("Try the Scanner tab to check a URL.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                }
            }
            .padding(.horizontal)
        }
        .padding(.top, 24)
    }
}

#Preview {
    DashboardView(showMenu: .constant(false))
        .environment(AppState())
}
