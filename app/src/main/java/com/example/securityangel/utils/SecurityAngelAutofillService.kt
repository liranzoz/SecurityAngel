package com.example.securityangel.utils

import android.app.PendingIntent
import android.app.assist.AssistStructure.ViewNode
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.example.securityangel.R
import com.example.securityangel.ui.autofill.AutofillSaveActivity
import com.example.securityangel.ui.autofill.AutofillUnlockActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SecurityAngelAutofillService : AutofillService() {

    companion object {
        private const val REQUEST_CODE_UNLOCK = 9001
    }

    // ── onFillRequest ─────────────────────────────────────────────────────────

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) { callback.onSuccess(null); return }

        val packageName = structure.activityComponent?.packageName ?: "unknown"
        Log.d("AutofillTracker", "onFillRequest started, package: $packageName")

        // Collect ALL nodes from every window in the structure first, then detect
        // the login pair once across the full set.  Calling detectLoginPair per
        // window was bug-prone: Chrome often splits virtual views across windows.
        val allNodes = mutableListOf<ViewNode>()
        for (i in 0 until structure.windowNodeCount) {
            if (cancellationSignal.isCanceled) { callback.onSuccess(null); return }
            collectNodes(structure.getWindowNodeAt(i).rootViewNode, allNodes)
        }

        val (usernameId, passwordId) = detectLoginPair(allNodes)

        if (passwordId == null) {
            Log.w("AutofillTracker", "Failed to find password node in package: $packageName")
        }

        if (usernameId == null && passwordId == null) {
            callback.onSuccess(null)
            return
        }

        // Domain: browsers expose webDomain on virtual form nodes; native apps don't.
        val domain = allNodes
            .firstNotNullOfOrNull { it.webDomain?.takeIf(String::isNotEmpty) }
            ?: packageName

        Log.d("AutofillTracker", "domain=$domain  usernameId=$usernameId  passwordId=$passwordId")

        val saveInfo = buildSaveInfo(usernameId, passwordId)

        val inlineSpecs: List<InlinePresentationSpec>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                request.inlineSuggestionsRequest?.inlinePresentationSpecs
            else null

        // Auth guard — vault locked
        if (!VaultSessionManager.isValid) {
            Log.d("AutofillTracker", "Vault locked — returning unlock dataset for domain=$domain")
            val responseBuilder = FillResponse.Builder().setSaveInfo(saveInfo)
            buildUnlockDataset(domain, usernameId, passwordId, inlineSpecs)?.let {
                responseBuilder.addDataset(it)
            }
            callback.onSuccess(responseBuilder.build())
            return
        }

        // Vault unlocked — query Firestore for matching credentials
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            callback.onSuccess(FillResponse.Builder().setSaveInfo(saveInfo).build())
            return
        }

        val masterPin = VaultSessionManager.masterPin
        val vaultSalt = VaultSessionManager.vaultSalt

        FirebaseFirestore.getInstance()
            .collection("users").document(userId).collection("vault")
            .get()
            .addOnSuccessListener { documents ->
                if (cancellationSignal.isCanceled) { callback.onSuccess(null); return@addOnSuccessListener }

                val responseBuilder = FillResponse.Builder().setSaveInfo(saveInfo)
                var datasetCount = 0

                for (doc in documents) {
                    val storedDomain = doc.getString("domain") ?: continue
                    if (!domainsMatch(domain, storedDomain)) continue

                    val email    = VaultCryptoManager.decrypt(doc.getString("email")    ?: "", masterPin, vaultSalt)
                    val password = VaultCryptoManager.decrypt(doc.getString("password") ?: "", masterPin, vaultSalt)
                    val siteName = doc.getString("siteName") ?: storedDomain

                    if (password.isEmpty()) continue

                    responseBuilder.addDataset(
                        buildCredentialDataset(siteName, email, password, usernameId, passwordId, inlineSpecs)
                    )
                    datasetCount++
                }

                Log.d("AutofillTracker", "onFillRequest: $datasetCount dataset(s) for domain=$domain")
                callback.onSuccess(responseBuilder.build())
            }
            .addOnFailureListener { e ->
                Log.e("AutofillTracker", "Firestore query failed: ${e.message}")
                callback.onSuccess(FillResponse.Builder().setSaveInfo(saveInfo).build())
            }
    }

    // ── onSaveRequest ─────────────────────────────────────────────────────────

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val contexts = request.fillContexts
        if (contexts.isEmpty()) {
            callback.onFailure("Security Angel: no fill contexts in save request")
            return
        }

        val packageName = contexts.last().structure.activityComponent?.packageName ?: ""
        Log.d("AutofillTracker", "onSaveRequest started, package: $packageName, contexts: ${contexts.size}")

        // Collect nodes from ALL historical contexts.  When a browser navigates
        // after form submit the filled nodes only exist in earlier contexts.
        val allNodes = mutableListOf<ViewNode>()
        for (context in contexts) {
            val structure = context.structure
            for (i in 0 until structure.windowNodeCount) {
                collectNodes(structure.getWindowNodeAt(i).rootViewNode, allNodes)
            }
        }

        val domain = allNodes
            .firstNotNullOfOrNull { it.webDomain?.takeIf(String::isNotEmpty) }
            ?: packageName

        var username = ""
        var password = ""

        // Pass 1 — HTML info + InputType (most reliable for browsers)
        val passwordNode = allNodes.firstOrNull { node ->
            node.autofillValue?.isText == true && isPasswordNode(node)
        }

        if (passwordNode != null) {
            password = passwordNode.autofillValue!!.textValue.toString()
            val pIndex = allNodes.indexOf(passwordNode)

            // Safe extraction: username is optional — saving with empty username
            // is better than crashing and losing the password entirely.
            username = allNodes.subList(0, pIndex)
                .lastOrNull { node ->
                    node.autofillValue?.isText == true &&
                    node.autofillValue!!.textValue.isNotEmpty() &&
                    isUsernameNode(node)
                }
                ?.autofillValue?.textValue?.toString() ?: ""
        }

        // Pass 2 — autofillHints fallback (reliable for native apps)
        if (password.isEmpty()) {
            for (node in allNodes) {
                val value = node.autofillValue?.takeIf { it.isText } ?: continue
                val text  = value.textValue.toString().takeIf { it.isNotEmpty() } ?: continue
                val hints = node.autofillHints ?: emptyArray()
                when {
                    isUsernameHint(hints) && username.isEmpty() -> username = text
                    isPasswordHint(hints) && password.isEmpty() -> password = text
                }
            }
        }

        Log.d("AutofillTracker", "onSaveRequest: domain=$domain  user='$username'  pass=${if (password.isEmpty()) "EMPTY" else "present"}")

        if (password.isEmpty()) {
            Log.w("AutofillTracker", "onSaveRequest: password not found — aborting save")
            callback.onFailure("Security Angel: could not extract password from form")
            return
        }

        val intent = Intent(this, AutofillSaveActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AutofillSaveActivity.EXTRA_USERNAME, username)
            putExtra(AutofillSaveActivity.EXTRA_PASSWORD, password)
            putExtra(AutofillSaveActivity.EXTRA_DOMAIN, domain)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, piFlags)
        callback.onSuccess(pendingIntent.intentSender)
    }

    // ── SaveInfo builder ──────────────────────────────────────────────────────

    private fun buildSaveInfo(usernameId: AutofillId?, passwordId: AutofillId?): SaveInfo {
        val requiredId = passwordId ?: usernameId!!
        val builder = SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
            arrayOf(requiredId)
        ).setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        if (requiredId === passwordId && usernameId != null)
            builder.setOptionalIds(arrayOf(usernameId))
        return builder.build()
    }

    // ── Dataset builders ──────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun buildCredentialDataset(
        siteName: String,
        email: String,
        password: String,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        inlineSpecs: List<InlinePresentationSpec>?
    ): Dataset {
        val views = buildDropdownPresentation(siteName, email.ifEmpty { siteName })
        val builder = Dataset.Builder(views)
        usernameId?.let { builder.setValue(it, AutofillValue.forText(email)) }
        passwordId?.let { builder.setValue(it, AutofillValue.forText(password)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !inlineSpecs.isNullOrEmpty()) {
            builder.setInlinePresentation(
                buildInlinePresentation(siteName, email.takeIf { it.isNotEmpty() }, inlineSpecs.first())
            )
        }
        return builder.build()
    }

    @Suppress("DEPRECATION")
    private fun buildUnlockDataset(
        domain: String,
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        inlineSpecs: List<InlinePresentationSpec>?
    ): Dataset? {
        val anyId = usernameId ?: passwordId ?: return null
        val views = buildDropdownPresentation("Security Angel", "Tap to unlock vault")
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        // AutofillUnlockActivity handles biometric/PIN unlock and returns a FillResponse
        // containing live datasets via AutofillManager.EXTRA_AUTHENTICATION_RESULT.
        val unlockIntent = Intent(this, AutofillUnlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AutofillUnlockActivity.EXTRA_DOMAIN, domain)
            putExtra(AutofillUnlockActivity.EXTRA_USERNAME_ID, usernameId)
            putExtra(AutofillUnlockActivity.EXTRA_PASSWORD_ID, passwordId)
        }
        val pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE_UNLOCK, unlockIntent, piFlags)
        val builder = Dataset.Builder(views)
            .setValue(anyId, AutofillValue.forText(""))
            .setAuthentication(pendingIntent.intentSender)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !inlineSpecs.isNullOrEmpty()) {
            builder.setInlinePresentation(
                buildInlinePresentation("Security Angel", "Tap to unlock", inlineSpecs.first())
            )
        }
        return builder.build()
    }

    private fun buildDropdownPresentation(title: String, subtitle: String): RemoteViews =
        RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
            setTextViewText(R.id.tv_autofill_title, title)
            setTextViewText(R.id.tv_autofill_subtitle, subtitle)
        }

    @RequiresApi(Build.VERSION_CODES.R)
    @Suppress("RestrictedApi")
    private fun buildInlinePresentation(
        title: String,
        subtitle: String?,
        spec: InlinePresentationSpec
    ): InlinePresentation {
        val pi = PendingIntent.getService(
            this, 0, Intent(this, SecurityAngelAutofillService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = InlineSuggestionUi.newContentBuilder(pi)
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setStartIcon(Icon.createWithResource(this, R.drawable.ic_password))
            .build()
        return InlinePresentation(content.slice, spec, false)
    }

    // ── Domain matching ───────────────────────────────────────────────────────

    internal fun domainsMatch(current: String, stored: String): Boolean {
        if (current.equals(stored, ignoreCase = true)) return true
        return rootDomain(current).equals(rootDomain(stored), ignoreCase = true)
    }

    internal fun rootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) "${parts[parts.size - 2]}.${parts[parts.size - 1]}" else domain
    }

    // ── Login-pair detection ──────────────────────────────────────────────────

    private fun detectLoginPair(nodes: List<ViewNode>): Pair<AutofillId?, AutofillId?> {
        val passwordNode = nodes.firstOrNull { it.autofillId != null && isPasswordNode(it) }

        val usernameNode = if (passwordNode != null) {
            // Standard form: username field appears before the password in the tree.
            val pIndex = nodes.indexOf(passwordNode)
            nodes.subList(0, pIndex).lastOrNull { it.autofillId != null && isUsernameNode(it) }
        } else {
            // No password field visible yet (email-first two-step flow, or sign-up
            // before the confirm-password field loads).  Return the best username
            // candidate so SaveInfo can still track the field.
            nodes.lastOrNull { it.autofillId != null && isUsernameNode(it) }
        }

        return Pair(usernameNode?.autofillId, passwordNode?.autofillId)
    }

    // ── Node classification ───────────────────────────────────────────────────

    private fun isPasswordNode(node: ViewNode): Boolean {
        node.htmlInfo?.attributes?.forEach { pair ->
            if (pair.first == "type" && pair.second == "password") return true
        }
        if (isPasswordInputType(node.inputType)) return true
        return isPasswordHint(node.autofillHints ?: emptyArray())
    }

    private fun isUsernameNode(node: ViewNode): Boolean {
        val inputType = node.inputType
        if (isPasswordInputType(inputType)) return false

        node.htmlInfo?.attributes?.forEach { pair ->
            when (pair.first) {
                "type" -> if (pair.second in listOf("email", "text", "tel")) return true
                "name", "id", "autocomplete" -> {
                    val v = pair.second.lowercase()
                    if (v.contains("user") || v.contains("email") ||
                        v.contains("login") || v.contains("phone")
                    ) return true
                }
            }
        }

        if (isUsernameInputType(inputType)) return true
        return isUsernameHint(node.autofillHints ?: emptyArray())
    }

    // ── InputType helpers ─────────────────────────────────────────────────────

    private fun isPasswordInputType(inputType: Int): Boolean {
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> true
            else -> false
        }
    }

    private fun isUsernameInputType(inputType: Int): Boolean {
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_NORMAL,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> true
            else -> false
        }
    }

    // ── Hint fallbacks ────────────────────────────────────────────────────────

    private fun isUsernameHint(hints: Array<String>): Boolean =
        hints.any { h ->
            h.equals(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
            h.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true) ||
            h.contains("user",  ignoreCase = true) ||
            h.contains("email", ignoreCase = true) ||
            h.contains("login", ignoreCase = true)
        }

    private fun isPasswordHint(hints: Array<String>): Boolean =
        hints.any { h ->
            h.equals(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) ||
            h.contains("pass", ignoreCase = true)
        }

    // ── Tree flattening ───────────────────────────────────────────────────────

    private fun collectNodes(node: ViewNode, out: MutableList<ViewNode>) {
        if (node.autofillId != null || node.inputType != 0 || node.htmlInfo != null) {
            val htmlAttrs = node.htmlInfo?.attributes
                ?.joinToString(", ") { "${it.first}=${it.second}" }
                ?: "null"
            val hints = node.autofillHints?.joinToString(", ") ?: "null"
            Log.d(
                "AutofillTracker",
                "Node: class=${node.className}" +
                    ", inputType=0x${node.inputType.toString(16)}" +
                    ", htmlInfo=[$htmlAttrs]" +
                    ", hints=[$hints]"
            )
        }
        out.add(node)
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { collectNodes(it, out) }
        }
    }
}
