import SwiftUI

struct ScoreRing: View {
    let score: Int
    var size: CGFloat = 200
    var lineWidth: CGFloat = 14
    @State private var animatedProgress: Double = 0

    private var tier: (color: Color, label: String) {
        switch score {
        case 80...:   return (Brand.accent, "Excellent")
        case 50..<80: return (.yellow, "Needs Attention")
        default:      return (.red, "At Risk")
        }
    }

    private var progress: Double { Double(score) / 100.0 }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.25), lineWidth: lineWidth)

            Circle()
                .trim(from: 0, to: animatedProgress)
                .stroke(
                    AngularGradient(
                        colors: [tier.color.opacity(0.6), tier.color, tier.color.opacity(0.9)],
                        center: .center
                    ),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .shadow(color: tier.color.opacity(0.5), radius: 12, x: 0, y: 0)

            VStack(spacing: 2) {
                Text("\(score)")
                    .font(Typography.scoreValue)
                    .foregroundStyle(.white)
                Text("Security Score")
                    .font(.caption.bold())
                    .foregroundStyle(.white.opacity(0.9))
                Text(tier.label)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.7))
            }
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(.spring(response: 1.2, dampingFraction: 0.85)) {
                animatedProgress = progress
            }
        }
        .onChange(of: score) { _, _ in
            withAnimation(.spring(response: 0.8, dampingFraction: 0.85)) {
                animatedProgress = progress
            }
        }
    }
}

#Preview {
    HStack(spacing: 20) {
        ScoreRing(score: 92, size: 160)
        ScoreRing(score: 64, size: 160)
        ScoreRing(score: 32, size: 160)
    }
    .padding()
    .background(Brand.headerGradient)
}
