package com.example.securityangel.ui.ai

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.data.models.ChatMessage
import com.example.securityangel.databinding.ActivityAiChatBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.ui.family.FamilySafetyActivity
import com.example.securityangel.ui.password.PasswordGeneratorActivity
import com.example.securityangel.ui.scanner.SandBoxActivity
import com.example.securityangel.ui.settings.SecurityLogActivity
import com.example.securityangel.ui.vault.PasswordVaultActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import com.example.securityangel.BuildConfig
import com.google.ai.client.generativeai.type.content
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIChatActivity : BaseActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var adapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var selectedImageUri: Uri? = null
    private val markwon by lazy { Markwon.create(this) }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showImagePreview(it)
        }
    }
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val chat = generativeModel.startChat(
        history = listOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarVisibility(false)

        buttonHandler()
        setupRecyclerView()
        loadChatHistory()

    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter
    }

    private fun sendMessage() {
        val userText = binding.etMessage.text.toString()
        val imageUri = selectedImageUri

        if (userText.isBlank() && imageUri == null) return

        val userMsg = ChatMessage(text = userText, isUser = true, imageUri = imageUri?.toString())
        adapter.addMessage(userMsg)
        saveMessageToFirebase(userMsg)
        binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)

        binding.etMessage.text.clear()
        binding.btnClosePreview.performClick()

        val loadingMsg = ChatMessage(isLoading = true)
        adapter.addMessage(loadingMsg)
        binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)

        buildSecurityContext { contextData ->
            lifecycleScope.launch {
                try {
                    val prompt = """
                        SYSTEM_CONTEXT:
                        $contextData

                        USER_INPUT:
                        $userText

                        INSTRUCTIONS:
                        If an image is provided, analyze it for phishing, scams, or sensitive data leaks.
                        If it's a screenshot of an email or SMS, check the sender and content for red flags.
                        Give a clear verdict: SAFE, SUSPICIOUS, or DANGEROUS. Consult to user what to do.
                    """.trimIndent()

                    val responseText = if (imageUri != null) {
                        generateResponseWithImage(prompt, imageUri)
                    } else {
                        generativeModel.generateContent(prompt).text ?: "No response."
                    }

                    adapter.removeLoadingMessage()
                    handleAiResponse(responseText)

                } catch (e: Exception) {
                    adapter.removeLoadingMessage()
                    addBotMessage("Connection error: ${e.message}")
                }
            }
        }
    }
    private suspend fun generateResponseWithImage(text: String, uri: Uri): String = withContext(
        Dispatchers.IO) {
        val bitmap = try {
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            throw Exception("Failed to process image")
        }

        val prompt = if (text.isEmpty()) "Analyze this image for security risks, phishing, or scams." else text

        val inputContent = content {
            image(bitmap)
            text(prompt)
        }

        val response = generativeModel.generateContent(inputContent)
        response.text ?: "Could not analyze the image."
    }
    private fun addBotMessage(text: String) {
        val botMsg = ChatMessage(text = text, isUser = false)
        adapter.addMessage(botMsg)
        saveMessageToFirebase(botMsg)
        binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)
    }

    private fun buildSecurityContext(onContextReady: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val userTask = db.collection("users").document(userId).get()
        val vaultTask = db.collection("users").document(userId).collection("vault").get()
        val scanTask = db.collection("users").document(userId).collection("scans")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1).get()
        val permissionsTask = db.collection("users").document(userId)
            .collection("permissions_risks").document("latest").get()

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(userTask, vaultTask, scanTask, permissionsTask)
            .addOnSuccessListener { results ->
                val userDoc        = results[0] as com.google.firebase.firestore.DocumentSnapshot
                val vaultDocs      = results[1] as com.google.firebase.firestore.QuerySnapshot
                val scanDocs       = results[2] as com.google.firebase.firestore.QuerySnapshot
                val permissionsDoc = results[3] as com.google.firebase.firestore.DocumentSnapshot

                val name = userDoc.getString("firstName") ?: "User"

                val risks = userDoc.get("activeRisks") as? List<String> ?: emptyList()
                val familyStatus = if (risks.isEmpty()) "Safe" else "At Risk (${risks.size} alerts)"

                val totalPasswords  = vaultDocs.size()
                val leakedPasswords = vaultDocs.documents.count { it.getBoolean("isLeaked") == true }

                val lastScanResult = if (scanDocs.isEmpty) "No scans yet" else {
                    val item = scanDocs.documents[0]
                    "${item.getString("url")} - ${if (item.getBoolean("isSafe") == true) "Safe" else "Malicious"}"
                }

                @Suppress("UNCHECKED_CAST")
                val rootStatusMap = permissionsDoc.get("rootStatus") as? Map<String, Any>
                val rootStatus = when {
                    rootStatusMap == null          -> "Unknown (no scan data yet)"
                    rootStatusMap["isRooted"] == true -> "ROOTED — device appears to be rooted"
                    else                           -> "Safe"
                }

                @Suppress("UNCHECKED_CAST")
                val riskyAppsList = permissionsDoc.get("riskyApps") as? List<Map<String, Any>> ?: emptyList()
                val riskyAppsText = if (riskyAppsList.isEmpty()) {
                    "None"
                } else {
                    riskyAppsList.joinToString("; ") { app ->
                        "${app["appName"]} [${app["riskLevel"]}]: ${app["riskExplanation"]}"
                    }
                }

                val contextPrompt = """
                    Current System Status for user $name:
                    - Total Passwords in Vault: $totalPasswords
                    - Compromised Passwords: $leakedPasswords
                    - Family Security Status: $familyStatus
                    - Last URL Scan: $lastScanResult
                    - Device Root Status: $rootStatus
                    - Risky Apps Installed: $riskyAppsText

                    You are an actionable assistant. You can perform actions inside the app.
                    If the user asks to do something, ANSWER normally, but append a special tag at the end.

                    Supported Actions:
                    1. To open password vault -> append [ACTION:OPEN_VAULT]
                    2. To check family status -> append [ACTION:OPEN_FAMILY]
                    3. To scan a website -> append [ACTION:OPEN_SCANNER]
                    4. To generate a password -> append [ACTION:GENERATE_PASS]
                    5. To view activity logs -> append [ACTION:OPEN_LOGS]

                    Example:
                    User: "I want to see my passwords"
                    AI: "Sure, taking you to your vault now. [ACTION:OPEN_VAULT]"
                """.trimIndent()

                onContextReady(contextPrompt)
            }
            .addOnFailureListener {
                onContextReady("Unable to fetch system status.")
            }
    }

    private fun handleAction(actionCode: String) {
        val intent = when (actionCode) {
            "OPEN_VAULT" -> android.content.Intent(this, PasswordVaultActivity::class.java)
            "OPEN_FAMILY" -> android.content.Intent(this, FamilySafetyActivity::class.java)
            "OPEN_SCANNER" -> android.content.Intent(this, SandBoxActivity::class.java)
            "GENERATE_PASS" -> android.content.Intent(this, PasswordGeneratorActivity::class.java)
            "OPEN_LOGS" -> android.content.Intent(this, SecurityLogActivity::class.java)
            else -> null
        }
        if (intent != null) {
            val friendlyName = actionCode.replace("OPEN_", "").replace("_", " ").lowercase().capitalize()

            showConfirmationDialog(friendlyName, intent)
        }
    }
    private fun showConfirmationDialog(screenName: String, targetIntent: android.content.Intent) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Action Required")
            .setMessage("Security Angel wants to take you to the $screenName screen. Proceed?")
            .setCancelable(false)
            .setPositiveButton("Take me") { dialog, _ ->
                startActivity(targetIntent)
                dialog.dismiss()
            }
            .setNegativeButton("No, Stay here") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun loadChatHistory() {
        if (userId == null) return

        db.collection("users").document(userId)
            .collection("chat_history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (doc in documents) {
                        val msg = doc.toObject(ChatMessage::class.java)
                        if (!msg.isLoading) {
                            adapter.addMessage(msg)
                        }
                    }
                    binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                } else {
                    addBotMessage("Hello! I am Security Angel AI. How can I help you stay safe today?")
                }
            }
            .addOnFailureListener {
                addBotMessage("Hello! I am ready to help.")
            }
    }

    private fun saveMessageToFirebase(message: ChatMessage) {
        if (userId == null || message.isLoading) return

        db.collection("users").document(userId)
            .collection("chat_history")
            .add(message)
    }

    override fun buttonHandler() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnClosePreview.setOnClickListener {
            clearImagePreview()
        }
    }

    private fun showImagePreview(uri: Uri) {
        binding.previewContainer.visibility = View.VISIBLE
        binding.ivPreview.setImageURI(uri)    }

    private fun clearImagePreview() {
        selectedImageUri = null
        binding.previewContainer.visibility = View.GONE
        binding.ivPreview.setImageDrawable(null)
    }

    private fun handleAiResponse(aiText: String) {
        var finalText = aiText
        val actionRegex = "\\[ACTION:([A-Z_]+)\\]".toRegex()
        val matchResult = actionRegex.find(finalText)

        if (matchResult != null) {
            val actionCode = matchResult.groupValues[1]
            finalText = finalText.replace(matchResult.value, "").trim()
            binding.root.postDelayed({ handleAction(actionCode) }, 1500)
        }

        addBotMessage(finalText)
    }
}
