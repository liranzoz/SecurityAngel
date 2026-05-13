import SwiftUI

struct DashboardView: View {
    @Binding var showMenu: Bool
    @State private var score: Int = 92
    private let user = MockData.currentUser
    private let recentScans = MockData.recentScans

    var body: some View {
        ZStack(alignment: .top) {
            Brand.backgroundGradient.ignoresSafeArea()

            Brand.headerGradient
                .frame(height: 380)
                .clipShape(.rect(bottomLeadingRadius: 36, bottomTrailingRadius: 36))
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
                .ignoresSafeArea(edges: .top)

            ScrollView {
                VStack(spacing: 0) {
                    header
                    tilesRow
                    recentScansSection
                    Spacer(minLength: 80)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .toolbarBackground(.hidden, for: .navigationBar)
        .statusBarHidden(false)
    }

    private var header: some View {
        VStack(spacing: 16) {
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
                    Text(user.firstName)
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
                .padding(.top, 12)
        }
        .padding(.top, 8)
        .frame(height: 380)
    }

    private var tilesRow: some View {
        HStack(spacing: 14) {
            tile(title: "My Vault", subtitle: "Manage Passwords", icon: "lock.shield.fill", tint: Brand.primary)
            tile(title: "Family Safety", subtitle: "Safe", icon: "person.3.fill", tint: Brand.accent)
        }
        .padding(.horizontal)
        .offset(y: -50)
        .padding(.bottom, -50)
    }

    private func tile(title: String, subtitle: String, icon: String, tint: Color) -> some View {
        TileCard {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 34, weight: .semibold))
                    .foregroundStyle(tint)
                    .padding(12)
                    .liquidGlass(in: Circle(), tint: tint.opacity(0.15))
                Spacer()
                Text(title)
                    .font(Typography.sectionTitle)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
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
}

#Preview {
    DashboardView(showMenu: .constant(false))
}
