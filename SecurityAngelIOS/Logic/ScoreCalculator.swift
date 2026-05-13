import Foundation

/// Mirrors Android's `GlobalScoreIntegrator`:
///
///   personalScore = max(20, 100 - 10 × leakedCount)
///   familyBlended = personalScore × 0.6 + avg(memberScores) × 0.4
///   permsSub      = 100 − 20·HIGH − 5·MED (cap 25) − 1·LOW (cap 10)
///   finalScore    = blended(weight=0.3, ceiling 75 if any HIGH)
enum ScoreCalculator {

    enum DashboardStatus: String {
        case excellent       = "Excellent"
        case needsAttention  = "Needs Attention"
        case atRisk          = "At Risk"
    }

    enum PermissionRisk: String { case safe = "SAFE", low = "LOW", medium = "MEDIUM", high = "HIGH" }

    struct Result {
        let finalScore: Int
        let status: DashboardStatus
        let permissionsSubScore: Int
        let summary: String
    }

    // MARK: - Personal score

    static func calculatePersonalScore(leakedPasswordCount: Int) -> Int {
        max(20, 100 - leakedPasswordCount * 10)
    }

    // MARK: - Family-blended (admin only)

    static func blendFamilyScore(myScore: Int, memberScores: [Int]) -> Int {
        guard !memberScores.isEmpty else { return myScore }
        let avg = Double(memberScores.reduce(0, +)) / Double(memberScores.count)
        let blended = Double(myScore) * 0.6 + avg * 0.4
        return min(100, max(0, Int(blended)))
    }

    // MARK: - Permissions / device-posture integration

    static func integratePermissionsScore(
        currentGlobalScore: Int,
        highRiskCount: Int,
        mediumRiskCount: Int,
        lowRiskCount: Int,
        permissionsWeight: Double = 0.3,
        highRiskCeiling: Int = 75
    ) -> Result {
        let highDeduction   = highRiskCount * 20
        let mediumDeduction = min(25, mediumRiskCount * 5)
        let lowDeduction    = min(10, lowRiskCount)
        let permsSub = max(0, 100 - highDeduction - mediumDeduction - lowDeduction)

        let weight = max(0, min(1, permissionsWeight))
        let blended = Double(currentGlobalScore) * (1 - weight) + Double(permsSub) * weight
        var final = Int(blended)
        if highRiskCount > 0 { final = min(final, highRiskCeiling) }
        final = max(0, min(100, final))

        let status: DashboardStatus = {
            switch final {
            case 90...:   return .excellent
            case 70..<90: return .needsAttention
            default:      return .atRisk
            }
        }()

        return Result(
            finalScore: final,
            status: status,
            permissionsSubScore: permsSub,
            summary: buildSummary(high: highRiskCount, medium: mediumRiskCount, low: lowRiskCount, final: final, capped: highRiskCount > 0)
        )
    }

    private static func buildSummary(high: Int, medium: Int, low: Int, final: Int, capped: Bool) -> String {
        if high == 0 && medium == 0 && low == 0 { return "All checks passed." }
        var parts: [String] = []
        if high   > 0 { parts.append("\(high) high-risk\(high > 1 ? "s" : "")") }
        if medium > 0 { parts.append("\(medium) medium-risk\(medium > 1 ? "s" : "")") }
        if low    > 0 { parts.append("\(low) low-risk\(low > 1 ? "s" : "")") }
        let list = parts.joined(separator: ", ")
        return capped ? "Score capped at \(final) — \(list) detected." : "Score affected by \(list)."
    }
}
