package com.example.securityangel.utils

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceSecuritySyncManager {

    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext

            val rootResult = RootDetector.getDetailedResult(context)
            val riskyApps = PermissionMonitor.getInstalledAppsWithPermissions(context)
                .filter {
                    it.riskLevel == PermissionMonitor.RiskLevel.HIGH ||
                    it.riskLevel == PermissionMonitor.RiskLevel.MEDIUM
                }

            val payload = mapOf(
                "rootStatus" to mapOf(
                    "isRooted"          to rootResult.isRooted,
                    "suBinariesFound"   to rootResult.suBinariesFound,
                    "testKeysFound"     to rootResult.testKeysFound,
                    "rootPackagesFound" to rootResult.rootPackagesFound
                ),
                "riskyApps" to riskyApps.map { app ->
                    mapOf(
                        "packageName"                to app.packageName,
                        "appName"                    to app.appName,
                        "riskLevel"                  to app.riskLevel.name,
                        "riskExplanation"            to app.riskExplanation,
                        "sensitivePermissionsSummary" to app.sensitivePermissionsSummary
                    )
                },
                "lastScanTimestamp" to System.currentTimeMillis()
            )

            Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("permissions_risks")
                    .document("latest")
                    .set(payload)
            )
        } catch (_: Exception) {

        }
    }
}
