package com.example.securityangel.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlin.math.ceil

object PermissionMonitor {

    // ─────────────────────────────────────────────────────────────────────────
    // Public model
    // ─────────────────────────────────────────────────────────────────────────

    enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }

    data class AppPermissionInfo(
        val packageName: String,
        val appName: String,
        val permissions: List<String>,
        val riskLevel: RiskLevel,
        val isGloballyTrusted: Boolean,
        val outOfContextPermissions: List<String>,
        val sensitivePermissionsSummary: String,
        val riskExplanation: String
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 2 – Trusted vendor prefixes
    // Any app whose package name starts with one of these belongs to a known
    // trusted ecosystem and is automatically rated SAFE.
    // ─────────────────────────────────────────────────────────────────────────

    private val TRUSTED_PREFIXES = listOf(
        "com.android.",        // Core Android / AOSP
        "com.google.",         // Google ecosystem
        "com.samsung.",        // Samsung OEM
        "com.sec.",            // Samsung OEM (secondary namespace)
        "com.miui.",           // Xiaomi / MIUI
        "com.xiaomi.",         // Xiaomi
        "com.microsoft.",      // Microsoft ecosystem
        "com.facebook.",       // Meta – Facebook
        "com.whatsapp.",       // Meta – WhatsApp
        "com.instagram.",      // Meta – Instagram
        "com.spotify.",        // Spotify
        "com.netflix.",        // Netflix
        "com.amazon.",         // Amazon ecosystem
        "com.adobe.",          // Adobe ecosystem
        "com.snapchat.",       // Snapchat
        "com.twitter.",        // Twitter / X
        "com.linkedin.",       // LinkedIn
        "org.telegram.",       // Telegram
        "org.thoughtcrime.",   // Signal
        "org.mozilla.",        // Mozilla / Firefox
        "com.discord.",        // Discord
        "com.dropbox.",        // Dropbox
        "com.huawei.",         // Huawei OEM
        "com.oneplus.",        // OnePlus OEM
        "com.oppo.",           // OPPO OEM
        "com.vivo.",           // Vivo OEM
        "com.realme."          // Realme OEM
    )

    // Exact-match fallback for well-known apps whose package names are NOT
    // covered by any prefix above (e.g. com.waze has no sub-package).
    private val TRUSTED_PACKAGES = hashSetOf(
        // Social / Messaging
        "com.waze",
        "com.viber.voip",

        "com.tumblr",
        "com.pinterest",
        "com.reddit.frontpage",
        "com.zhiliaoapp.musically",        // TikTok
        "com.kwai.video",                  // Kwai
        // Streaming / Entertainment
        "com.hbo.hbonow",
        "com.disneyplus",
        "tv.twitch.android.app",
        "com.twitch.android.app",
        "com.soundcloud.android",
        "com.deezer.android.app",
        // Navigation & Travel
        "com.here.app.maps",
        "com.booking",
        "com.airbnb.android",
        "com.tripadvisor.tripadvisor",
        // Finance & Payments
        "com.paypal.android.p2pmobile",
        "com.venmo",
        "com.squareup.cash",
        "com.coinbase.android",
        "com.robinhood.android",
        // Productivity & Cloud
        "com.evernote",
        "com.box.android",
        "com.github.android",
        // Password managers
        "com.lastpass.lpandroid",
        "com.callpod.android_apps.keeper",
        "com.agilebits.onepassword",
        // Ride-sharing & Food
        "com.ubercab",
        "com.ubercab.eats",
        "com.lyft",
        "com.doordash.consumer",
        "com.grubhub.android",
        // Communication
        "us.zoom.videomeetings",
        "com.Slack",
        // Browsers not covered by prefix
        "com.opera.browser",
        "com.brave.browser",
        "com.kiwibrowser.browser",
        // Games
        "com.mojang.minecraftpe",
        "com.supercell.clashofclans",
        "com.king.candycrushsaga",
        "com.roblox.client",
        // Misc
        "com.duolingo",
        "com.tinder",
        "com.bumble.app",
        "com.canva.editor",
        "com.truecaller",
        "com.shazam.android",
        "com.getsomeheadspace.android",
        "com.calm.android",
        "com.ebay.mobile"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Category baselines
    // Permissions that are EXPECTED and LEGITIMATE for each app category.
    // ─────────────────────────────────────────────────────────────────────────

    private val CATEGORY_BASELINES: Map<Int, Set<String>> = mapOf(
        ApplicationInfo.CATEGORY_GAME to setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT"
        ),
        ApplicationInfo.CATEGORY_AUDIO to setOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_CONNECT"
        ),
        ApplicationInfo.CATEGORY_VIDEO to setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE"
        ),
        ApplicationInfo.CATEGORY_IMAGE to setOf(
            "android.permission.CAMERA",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.INTERNET",
            "android.permission.ACCESS_MEDIA_LOCATION",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT"
        ),
        ApplicationInfo.CATEGORY_SOCIAL to setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT",
            "android.permission.GET_ACCOUNTS",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.ACTIVITY_RECOGNITION"
        ),
        ApplicationInfo.CATEGORY_NEWS to setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WAKE_LOCK",
            "android.permission.VIBRATE"
        ),
        ApplicationInfo.CATEGORY_MAPS to setOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.VIBRATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CONTACTS",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.FOREGROUND_SERVICE"
        ),
        ApplicationInfo.CATEGORY_PRODUCTIVITY to setOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.GET_ACCOUNTS",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.READ_PHONE_STATE"
        )
    )

    private val DEFAULT_BASELINE = setOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.VIBRATE",
        "android.permission.WAKE_LOCK",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // OCP weights
    // ─────────────────────────────────────────────────────────────────────────

    private val OCP_WEIGHTS = mapOf(
        "android.permission.CAMERA"                   to 3,
        "android.permission.RECORD_AUDIO"             to 3,
        "android.permission.ACCESS_FINE_LOCATION"     to 3,
        "android.permission.ACCESS_COARSE_LOCATION"   to 2,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 4,
        "android.permission.READ_CONTACTS"            to 3,
        "android.permission.WRITE_CONTACTS"           to 2,
        "android.permission.READ_CALL_LOG"            to 4,
        "android.permission.WRITE_CALL_LOG"           to 3,
        "android.permission.PROCESS_OUTGOING_CALLS"   to 4,
        "android.permission.READ_SMS"                 to 4,
        "android.permission.RECEIVE_SMS"              to 3,
        "android.permission.SEND_SMS"                 to 4,
        "android.permission.READ_PHONE_STATE"         to 2,
        "android.permission.CALL_PHONE"               to 3,
        "android.permission.GET_ACCOUNTS"             to 2,
        "android.permission.USE_BIOMETRIC"            to 1,
        "android.permission.USE_FINGERPRINT"          to 1,
        "android.permission.SYSTEM_ALERT_WINDOW"      to 3,
        "android.permission.WRITE_SETTINGS"           to 2,
        "android.permission.REQUEST_INSTALL_PACKAGES" to 3,
        "android.permission.PACKAGE_USAGE_STATS"      to 3,
        "android.permission.READ_EXTERNAL_STORAGE"    to 1,
        "android.permission.WRITE_EXTERNAL_STORAGE"   to 1,
        "android.permission.MANAGE_EXTERNAL_STORAGE"  to 3,
        "android.permission.BODY_SENSORS"             to 2,
        "android.permission.ACTIVITY_RECOGNITION"     to 2,
        "android.permission.READ_CALENDAR"            to 2,
        "android.permission.WRITE_CALENDAR"           to 2
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Malicious combinations engine
    // requiredInOcp – must be in the OCP set to trigger
    // requiredInAll – must be anywhere in the app's permissions (covers ubiquitous
    //                 enablers like INTERNET that are never an OCP)
    // ─────────────────────────────────────────────────────────────────────────

    private data class PermissionCombo(
        val requiredInOcp: Set<String>,
        val requiredInAll: Set<String> = emptySet(),
        val description: String,
        val score: Int
    )

    private val MALICIOUS_COMBOS = listOf(
        PermissionCombo(
            requiredInOcp = setOf("android.permission.READ_SMS"),
            requiredInAll = setOf("android.permission.INTERNET"),
            description = "Possible SMS data exfiltration",
            score = 8
        ),
        PermissionCombo(
            requiredInOcp = setOf("android.permission.READ_CONTACTS"),
            requiredInAll = setOf("android.permission.INTERNET"),
            description = "Possible contact list harvesting",
            score = 6
        ),
        PermissionCombo(
            requiredInOcp = setOf("android.permission.READ_CALL_LOG"),
            requiredInAll = setOf("android.permission.INTERNET"),
            description = "Possible call log exfiltration",
            score = 8
        ),
        PermissionCombo(
            requiredInOcp = setOf(
                "android.permission.CAMERA",
                "android.permission.SYSTEM_ALERT_WINDOW"
            ),
            description = "Stealth camera access via screen overlay",
            score = 10
        ),
        PermissionCombo(
            requiredInOcp = setOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.SYSTEM_ALERT_WINDOW"
            ),
            description = "Stealth location tracking via screen overlay",
            score = 10
        ),
        PermissionCombo(
            requiredInOcp = setOf("android.permission.REQUEST_INSTALL_PACKAGES"),
            requiredInAll = setOf("android.permission.INTERNET"),
            description = "Can silently download and install packages",
            score = 9
        ),
        PermissionCombo(
            requiredInOcp = setOf("android.permission.RECORD_AUDIO"),
            requiredInAll = setOf("android.permission.INTERNET"),
            description = "Possible audio surveillance",
            score = 8
        ),
        PermissionCombo(
            requiredInOcp = setOf(
                "android.permission.PROCESS_OUTGOING_CALLS",
                "android.permission.RECORD_AUDIO"
            ),
            description = "Can intercept and record phone calls",
            score = 10
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Trusted installation sources
    // ─────────────────────────────────────────────────────────────────────────

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps",
        "com.huawei.appmarket",
        "com.xiaomi.market",
        "com.oppo.market",
        "com.bbk.appstore",
        "com.microsoft.windowsstore"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Human-readable permission labels
    // ─────────────────────────────────────────────────────────────────────────

    private val PERMISSION_LABELS = mapOf(
        "android.permission.CAMERA"                   to "Camera",
        "android.permission.RECORD_AUDIO"             to "Microphone",
        "android.permission.ACCESS_FINE_LOCATION"     to "Location",
        "android.permission.ACCESS_COARSE_LOCATION"   to "Approx. Location",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Background Location",
        "android.permission.READ_CONTACTS"            to "Contacts",
        "android.permission.WRITE_CONTACTS"           to "Write Contacts",
        "android.permission.READ_CALL_LOG"            to "Call Log",
        "android.permission.WRITE_CALL_LOG"           to "Write Call Log",
        "android.permission.PROCESS_OUTGOING_CALLS"   to "Intercept Calls",
        "android.permission.READ_SMS"                 to "Read SMS",
        "android.permission.RECEIVE_SMS"              to "Receive SMS",
        "android.permission.SEND_SMS"                 to "Send SMS",
        "android.permission.READ_PHONE_STATE"         to "Phone State",
        "android.permission.CALL_PHONE"               to "Make Calls",
        "android.permission.GET_ACCOUNTS"             to "Accounts",
        "android.permission.SYSTEM_ALERT_WINDOW"      to "Draw Overlay",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Install Apps",
        "android.permission.PACKAGE_USAGE_STATS"      to "Usage Stats",
        "android.permission.READ_EXTERNAL_STORAGE"    to "Read Storage",
        "android.permission.WRITE_EXTERNAL_STORAGE"   to "Write Storage",
        "android.permission.MANAGE_EXTERNAL_STORAGE"  to "Full Storage",
        "android.permission.USE_BIOMETRIC"            to "Biometric",
        "android.permission.WRITE_SETTINGS"           to "Modify Settings",
        "android.permission.BODY_SENSORS"             to "Body Sensors",
        "android.permission.ACTIVITY_RECOGNITION"     to "Activity Recognition",
        "android.permission.READ_CALENDAR"            to "Calendar",
        "android.permission.WRITE_CALENDAR"           to "Write Calendar"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun getBaseline(category: Int): Set<String> =
        CATEGORY_BASELINES[category] ?: DEFAULT_BASELINE

    private fun effectiveCategory(declared: Int, permSet: Set<String>): Int {
        if (declared != ApplicationInfo.CATEGORY_UNDEFINED) return declared
        val hasCalendar = permSet.any { "CALENDAR" in it }
        val hasCallLog  = permSet.any { "CALL_LOG" in it || "OUTGOING_CALLS" in it }
        val hasContacts = "android.permission.READ_CONTACTS" in permSet
        val hasCamera   = "android.permission.CAMERA" in permSet
        val hasAudio    = "android.permission.RECORD_AUDIO" in permSet
        val hasLocation = "android.permission.ACCESS_FINE_LOCATION" in permSet
        return when {
            hasCalendar                          -> ApplicationInfo.CATEGORY_PRODUCTIVITY
            hasCallLog && hasContacts            -> ApplicationInfo.CATEGORY_SOCIAL
            hasContacts && hasCamera && hasAudio -> ApplicationInfo.CATEGORY_SOCIAL
            hasLocation && hasCamera             -> ApplicationInfo.CATEGORY_MAPS
            hasCamera && !hasContacts            -> ApplicationInfo.CATEGORY_IMAGE
            hasAudio                             -> ApplicationInfo.CATEGORY_AUDIO
            else                                 -> ApplicationInfo.CATEGORY_UNDEFINED
        }
    }

    private fun categoryName(category: Int): String = when (category) {
        ApplicationInfo.CATEGORY_GAME         -> "Game"
        ApplicationInfo.CATEGORY_AUDIO        -> "Audio/Music app"
        ApplicationInfo.CATEGORY_VIDEO        -> "Video app"
        ApplicationInfo.CATEGORY_IMAGE        -> "Photo/Image app"
        ApplicationInfo.CATEGORY_SOCIAL       -> "Social app"
        ApplicationInfo.CATEGORY_NEWS         -> "News app"
        ApplicationInfo.CATEGORY_MAPS         -> "Maps/Navigation app"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity app"
        else                                  -> "app"
    }

    private fun getInstallerPackage(pm: PackageManager, packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (_: Exception) {
        null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OCP assessment (only called after all trust tiers pass)
    // ─────────────────────────────────────────────────────────────────────────

    private data class AssessmentResult(
        val riskLevel: RiskLevel,
        val ocps: List<String>,
        val summary: String,
        val explanation: String
    )

    private fun assess(
        packageName: String,
        permissions: List<String>,
        category: Int,
        pm: PackageManager
    ): AssessmentResult {
        val permSet = permissions.toSet()
        val resolvedCategory = effectiveCategory(category, permSet)
        val baseline = getBaseline(resolvedCategory)

        val ocpSet = permSet
            .filter { it in OCP_WEIGHTS && it !in baseline }
            .toSet()

        val triggeredCombos = MALICIOUS_COMBOS.filter { combo ->
            combo.requiredInOcp.all { it in ocpSet } &&
            combo.requiredInAll.all { it in permSet }
        }

        // null installer = pre-installed by OEM; only explicitly non-store installers are sideloaded
        val installer = getInstallerPackage(pm, packageName)
        val isSideloaded = installer != null && installer !in TRUSTED_INSTALLERS

        val ocpScore   = ocpSet.sumOf { OCP_WEIGHTS[it] ?: 0 }
        val comboScore = triggeredCombos.sumOf { it.score }
        val rawScore   = ocpScore + comboScore
        val finalScore = if (isSideloaded && rawScore > 0) ceil(rawScore * 1.5).toInt() else rawScore

        val riskLevel = when {
            finalScore >= 10 -> RiskLevel.HIGH
            finalScore >= 5  -> RiskLevel.MEDIUM
            finalScore >= 1  -> RiskLevel.LOW
            else             -> RiskLevel.SAFE
        }

        val ocpLabels   = ocpSet.mapNotNull { PERMISSION_LABELS[it] }.sorted()
        val explanation = buildExplanation(
            categoryLabel     = categoryName(resolvedCategory),
            ocpLabels         = ocpLabels,
            comboDescriptions = triggeredCombos.map { it.description },
            isSideloaded      = isSideloaded,
            riskLevel         = riskLevel
        )

        return AssessmentResult(
            riskLevel = riskLevel,
            ocps      = ocpSet.toList(),
            summary   = ocpLabels.take(3).joinToString(" · "),
            explanation = explanation
        )
    }

    private fun buildExplanation(
        categoryLabel: String,
        ocpLabels: List<String>,
        comboDescriptions: List<String>,
        isSideloaded: Boolean,
        riskLevel: RiskLevel
    ): String {
        if (riskLevel == RiskLevel.SAFE) return "No suspicious permissions detected."
        val parts = mutableListOf<String>()
        if (ocpLabels.isNotEmpty())
            parts += "Categorized as a $categoryLabel but requests: ${ocpLabels.joinToString(", ")}"
        comboDescriptions.forEach { parts += it }
        if (isSideloaded) parts += "Installed from an unknown source (not Play Store)"
        return parts.joinToString(". ").trimEnd('.') + "."
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun openAppPermissionSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getInstalledAppsWithPermissions(context: Context): List<AppPermissionInfo> {
        val pm          = context.packageManager
        val hostPackage = context.packageName

        val launcherPackages = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.toSet()

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                // ── Tier 1: System / OS-level apps – drop entirely ──────────
                val isSystemApp = (appInfo.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                if (isSystemApp) return@filter false

                // Only keep apps the user can see in the launcher
                appInfo.packageName in launcherPackages
            }
            .map { appInfo ->
                val pkg = appInfo.packageName

                val permissions = try {
                    pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
                        .requestedPermissions?.toList() ?: emptyList()
                } catch (_: PackageManager.NameNotFoundException) {
                    emptyList()
                }

                // ── Tier 2: Trusted vendor prefix ───────────────────────────
                val isTrustedByPrefix = TRUSTED_PREFIXES.any { pkg.startsWith(it) }

                // ── Tier 3: Self-exclusion ───────────────────────────────────
                val isSelf = pkg == hostPackage

                if (isSelf || isTrustedByPrefix || pkg in TRUSTED_PACKAGES) {
                    AppPermissionInfo(
                        packageName             = pkg,
                        appName                 = pm.getApplicationLabel(appInfo).toString(),
                        permissions             = permissions,
                        riskLevel               = RiskLevel.SAFE,
                        isGloballyTrusted       = true,
                        outOfContextPermissions = emptyList(),
                        sensitivePermissionsSummary = "",
                        riskExplanation         = if (isSelf)
                            "This is the host security application."
                        else
                            "Recognized as a trusted application."
                    )
                } else {
                    val result = assess(
                        packageName = pkg,
                        permissions = permissions,
                        category    = appInfo.category,
                        pm          = pm
                    )
                    AppPermissionInfo(
                        packageName             = pkg,
                        appName                 = pm.getApplicationLabel(appInfo).toString(),
                        permissions             = permissions,
                        riskLevel               = result.riskLevel,
                        isGloballyTrusted       = false,
                        outOfContextPermissions = result.ocps,
                        sensitivePermissionsSummary = result.summary,
                        riskExplanation         = result.explanation
                    )
                }
            }
            .sortedWith(
                compareByDescending<AppPermissionInfo> { it.riskLevel.ordinal }
                    .thenBy { it.appName }
            )
    }
}
