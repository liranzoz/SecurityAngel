package com.example.securityangel.utils

import com.example.securityangel.utils.PermissionMonitor.AppPermissionInfo
import com.example.securityangel.utils.PermissionMonitor.RiskLevel

enum class DashboardStatus(val label: String) {
    EXCELLENT("Excellent"),
    NEEDS_ATTENTION("Needs Attention"),
    AT_RISK("At Risk")
}

object GlobalScoreIntegrator {

    private const val BREACH_DEDUCTION_PER_LEAK = 10
    private const val BREACH_SCORE_FLOOR        = 20

    private const val FAMILY_MY_WEIGHT     = 0.6
    private const val FAMILY_MEMBER_WEIGHT = 0.4

    private const val HIGH_DEDUCTION_PER_APP   = 20
    private const val MEDIUM_DEDUCTION_PER_APP = 5
    private const val LOW_DEDUCTION_PER_APP    = 1

    private const val LOW_DEDUCTION_CAP    = 10
    private const val MEDIUM_DEDUCTION_CAP = 25

    data class IntegratedScoreResult(
        val finalScore: Int,
        val status: DashboardStatus,
        val permissionsSubScore: Int,
        val summaryMessage: String
    )

    fun calculatePersonalScore(leakedPasswordCount: Int): Int =
        (100 - leakedPasswordCount * BREACH_DEDUCTION_PER_LEAK)
            .coerceAtLeast(BREACH_SCORE_FLOOR)

    fun blendFamilyScore(myScore: Int, memberScores: List<Int>): Int {
        if (memberScores.isEmpty()) return myScore
        val familyAverage = memberScores.average()
        return (myScore * FAMILY_MY_WEIGHT + familyAverage * FAMILY_MEMBER_WEIGHT)
            .toInt()
            .coerceIn(0, 100)
    }

    fun integratePermissionsScore(
        currentGlobalScore: Int,
        apps: List<AppPermissionInfo>,
        permissionsWeight: Float = 0.3f,
        highRiskCeiling: Int = 75
    ): IntegratedScoreResult {

        val highCount   = apps.count { it.riskLevel == RiskLevel.HIGH }
        val mediumCount = apps.count { it.riskLevel == RiskLevel.MEDIUM }
        val lowCount    = apps.count { it.riskLevel == RiskLevel.LOW }

        val highDeduction   = highCount * HIGH_DEDUCTION_PER_APP
        val mediumDeduction = (mediumCount * MEDIUM_DEDUCTION_PER_APP)
            .coerceAtMost(MEDIUM_DEDUCTION_CAP)
        val lowDeduction    = (lowCount * LOW_DEDUCTION_PER_APP)
            .coerceAtMost(LOW_DEDUCTION_CAP)

        val permissionsSubScore = (100 - highDeduction - mediumDeduction - lowDeduction)
            .coerceAtLeast(0)

        val weight       = permissionsWeight.coerceIn(0f, 1f)
        val blendedScore = (currentGlobalScore * (1f - weight) +
                            permissionsSubScore * weight).toInt()

        val finalScore = (if (highCount > 0) blendedScore.coerceAtMost(highRiskCeiling)
                          else blendedScore)
            .coerceIn(0, 100)

        val status = when {
            finalScore >= 90 -> DashboardStatus.EXCELLENT
            finalScore >= 70 -> DashboardStatus.NEEDS_ATTENTION
            else             -> DashboardStatus.AT_RISK
        }

        return IntegratedScoreResult(
            finalScore          = finalScore,
            status              = status,
            permissionsSubScore = permissionsSubScore,
            summaryMessage      = buildSummary(highCount, mediumCount, lowCount, finalScore, highCount > 0)
        )
    }

    private fun buildSummary(
        highCount: Int,
        mediumCount: Int,
        lowCount: Int,
        finalScore: Int,
        ceilingApplied: Boolean
    ): String {
        if (highCount == 0 && mediumCount == 0 && lowCount == 0)
            return "All scanned apps have safe permissions."

        val parts = buildList {
            if (highCount   > 0) add("$highCount high-risk app${if (highCount   > 1) "s" else ""}")
            if (mediumCount > 0) add("$mediumCount medium-risk app${if (mediumCount > 1) "s" else ""}")
            if (lowCount    > 0) add("$lowCount low-risk app${if (lowCount    > 1) "s" else ""}")
        }

        val appList = parts.joinToString(", ")

        return if (ceilingApplied)
            "Score capped at $finalScore — $appList detected with suspicious permissions."
        else
            "Score affected by $appList."
    }

    fun integratePermissionsScore(
        currentGlobalScore: Int,
        highRiskCount: Int,
        mediumRiskCount: Int,
        lowRiskCount: Int,
        permissionsWeight: Float = 0.3f,
        highRiskCeiling: Int = 75
    ): IntegratedScoreResult {
        val syntheticApps = buildList {
            repeat(highRiskCount)   { add(syntheticApp(RiskLevel.HIGH)) }
            repeat(mediumRiskCount) { add(syntheticApp(RiskLevel.MEDIUM)) }
            repeat(lowRiskCount)    { add(syntheticApp(RiskLevel.LOW)) }
        }
        return integratePermissionsScore(
            currentGlobalScore, syntheticApps, permissionsWeight, highRiskCeiling
        )
    }

    private fun syntheticApp(level: RiskLevel) = AppPermissionInfo(
        packageName             = "",
        appName                 = "",
        permissions             = emptyList(),
        riskLevel               = level,
        isGloballyTrusted       = false,
        outOfContextPermissions = emptyList(),
        sensitivePermissionsSummary = "",
        riskExplanation         = ""
    )
}
