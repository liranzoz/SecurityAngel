package com.example.securityangel.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object RootDetector {

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/system/xbin/mu",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/su/bin/su"
    )

    private val ROOT_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot",
        "com.saurik.substrate",
        "de.robv.android.xposed.installer",
        "com.android.settings"
    )

    fun isRooted(context: Context): Boolean {
        return hasSuBinaries() || hasTestKeys() || hasRootPackages(context)
    }

    fun hasSuBinaries(): Boolean {
        return SU_PATHS.any { java.io.File(it).exists() }
    }

    fun hasTestKeys(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    fun hasRootPackages(context: Context): Boolean {
        val pm = context.packageManager
        return ROOT_PACKAGES.any { pkg ->
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    data class RootCheckResult(
        val isRooted: Boolean,
        val suBinariesFound: Boolean,
        val testKeysFound: Boolean,
        val rootPackagesFound: Boolean
    )

    fun getDetailedResult(context: Context): RootCheckResult {
        val suBinaries = hasSuBinaries()
        val testKeys = hasTestKeys()
        val rootPackages = hasRootPackages(context)
        return RootCheckResult(
            isRooted = suBinaries || testKeys || rootPackages,
            suBinariesFound = suBinaries,
            testKeysFound = testKeys,
            rootPackagesFound = rootPackages
        )
    }
}