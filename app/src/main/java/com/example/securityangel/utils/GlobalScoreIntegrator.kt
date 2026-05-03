package com.example.securityangel.utils

import com.example.securityangel.utils.PermissionMonitor.AppPermissionInfo
import com.example.securityangel.utils.PermissionMonitor.RiskLevel

// Top-level so DashboardActivity and any other screen can import it directly.
enum class DashboardStatus(val label: String) {
    EXCELLENT("Excellent"),
    NEEDS_ATTENTION("Needs Attention"),
    AT_RISK("At Risk")
}

object GlobalScoreIntegrator {

    // ── Personal breach score ─────────────────────────────────────────────────
    private const val BREACH_DEDUCTION_PER_LEAK = 10
    private const val BREACH_SCORE_FLOOR        = 20

    // ── Family blend weights ──────────────────────────────────────────────────
    private const val FAMILY_MY_WEIGHT     = 0.6
    private const val FAMILY_MEMBER_WEIGHT = 0.4

    // ── Permissions deduction constants ──────────────────────────────────────
    private const val HIGH_DEDUCTION_PER_APP   = 20
    private const val MEDIUM_DEDUCTION_PER_APP = 5
    private const val LOW_DEDUCTION_PER_APP    = 1

    // Per-tier caps (HIGH is uncapped by design — one rogue app matters)
    private const val LOW_DEDUCTION_CAP    = 10
    private const val MEDIUM_DEDUCTION_CAP = 25

    // ── Output model ─────────────────────────────────────────────────────────

    data class IntegratedScoreResult(
        val finalScore: Int,           // 0-100, clamped
        val status: DashboardStatus,
        val permissionsSubScore: Int,  // raw permissions score before blending
        val summaryMessage: String
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Personal breach score
    // Starts at 100, deducts per leaked vault password, hard floor at 20.
    // ─────────────────────────────────────────────────────────────────────────
    fun calculatePersonalScore(leakedPasswordCount: Int): Int =
        (100 - leakedPasswordCount * BREACH_DEDUCTION_PER_LEAK)
            .coerceAtLeast(BREACH_SCORE_FLOOR)

    // ─────────────────────────────────────────────────────────────────────────
    // Family weighted blend  (admin-only path)
    // myScore × 60 % + average of all other members × 40 %.
    // Returns myScore unchanged when the family list is empty.
    // ─────────────────────────────────────────────────────────────────────────
    fun blendFamilyScore(myScore: Int, memberScores: List<Int>): Int {
        if (memberScores.isEmpty()) return myScore
        val familyAverage = memberScores.average()
        return (myScore * FAMILY_MY_WEIGHT + familyAverage * FAMILY_MEMBER_WEIGHT)
            .toInt()
            .coerceIn(0, 100)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main integration function
    //
    // currentGlobalScore  – score already computed by the existing breach/family
    //                        engine (0-100)
    // apps                – output of PermissionMonitor.getInstalledAppsWithPermissions()
    // permissionsWeight   – fraction of the total score that permissions own (0..1)
    // highRiskCeiling     – hard cap applied when any HIGH-risk app is present
    // ─────────────────────────────────────────────────────────────────────────
    fun integratePermissionsScore(
        currentGlobalScore: Int,
        apps: List<AppPermissionInfo>,
        permissionsWeight: Float = 0.3f,
        highRiskCeiling: Int = 75
    ): IntegratedScoreResult {

        // ── Step 1: Tally risk counts (exclude SAFE — they cost nothing) ─────
        val highCount   = apps.count { it.riskLevel == RiskLevel.HIGH }
        val mediumCount = apps.count { it.riskLevel == RiskLevel.MEDIUM }
        val lowCount    = apps.count { it.riskLevel == RiskLevel.LOW }

        // ── Step 2: Permissions sub-score (0-100) ────────────────────────────
        val highDeduction   = highCount * HIGH_DEDUCTION_PER_APP              // no cap
        val mediumDeduction = (mediumCount * MEDIUM_DEDUCTION_PER_APP)
            .coerceAtMost(MEDIUM_DEDUCTION_CAP)
        val lowDeduction    = (lowCount * LOW_DEDUCTION_PER_APP)
            .coerceAtMost(LOW_DEDUCTION_CAP)

        val permissionsSubScore = (100 - highDeduction - mediumDeduction - lowDeduction)
            .coerceAtLeast(0)

        // ── Step 3: Weighted blend with the existing global score ─────────────
        val weight       = permissionsWeight.coerceIn(0f, 1f)
        val blendedScore = (currentGlobalScore * (1f - weight) +
                            permissionsSubScore * weight).toInt()

        // ── Step 4: Critical override — any HIGH app caps the result ──────────
        val finalScore = (if (highCount > 0) blendedScore.coerceAtMost(highRiskCeiling)
                          else blendedScore)
            .coerceIn(0, 100)

        // ── Step 5: Status mapping ────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

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

    // Convenience overload: accepts pre-counted values instead of the full list.
    // Useful when the UI already has counts cached and wants to avoid re-scanning.
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
