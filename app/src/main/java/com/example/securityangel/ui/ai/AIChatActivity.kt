package com.example.securityangel.ui.ai

import android.os.Bundle
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

class AIChatActivity : BaseActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var adapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = "AIzaSyDVtzG9ncdCEuCgu-un6DxSjsv_cj1-6Ek"
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
//        addBotMessage("Hello! I am Security Angel AI. How can I help you stay safe today?")
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter
    }

    private fun sendMessage() {
        val userText = binding.etMessage.text.toString()
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isUser = true)
        adapter.addMessage(userMsg)
        saveMessageToFirebase(userMsg)
        binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)
        binding.etMessage.text.clear()

        val loadingMsg = ChatMessage(
            text = "",
            isUser = false,
            isLoading = true
        )

        adapter.addMessage(loadingMsg)
        binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)
        buildSecurityContext { contextData ->

            lifecycleScope.launch {
                try {
                    val fullPrompt = """
                        SYSTEM_CONTEXT:
                        $contextData
                        
                        USER_QUESTION:
                        $userText
                    """.trimIndent()

                    val response = chat.sendMessage(fullPrompt)
                    var aiText = response.text ?: "I'm sorry, I couldn't process that."
                    adapter.removeLoadingMessage()

                    val actionRegex = "\\[ACTION:([A-Z_]+)\\]".toRegex()
                    val matchResult = actionRegex.find(aiText)

                    if (matchResult != null) {
                        val actionCode = matchResult.groupValues[1]

                        aiText = aiText.replace(matchResult.value, "").trim()

                        binding.root.postDelayed({
                            handleAction(actionCode)
                        }, 1500)
                    }

                    addBotMessage(aiText)

                } catch (e: Exception) {
                    adapter.removeLoadingMessage()
                    addBotMessage("Sorry, I'm having trouble connecting. Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
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

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(userTask, vaultTask, scanTask)
            .addOnSuccessListener { results ->
                val userDoc = results[0] as com.google.firebase.firestore.DocumentSnapshot
                val vaultDocs = results[1] as com.google.firebase.firestore.QuerySnapshot
                val scanDocs = results[2] as com.google.firebase.firestore.QuerySnapshot


                val name = userDoc.getString("firstName") ?: "User"

                val risks = userDoc.get("activeRisks") as? List<String> ?: emptyList()
                val familyStatus = if (risks.isEmpty()) "Safe" else "At Risk (${risks.size} alerts)"

                val totalPasswords = vaultDocs.size()
                val leakedPasswords = vaultDocs.documents.count { it.getBoolean("isLeaked") == true }

                val lastScanResult = if (scanDocs.isEmpty) "No scans yet" else {
                    val item = scanDocs.documents[0]
                    "${item.getString("url")} - ${if (item.getBoolean("isSafe") == true) "Safe" else "Malicious"}"
                }

                val contextPrompt = """
                    Current System Status for user $name:
                    - Total Passwords in Vault: $totalPasswords
                    - Compromised Passwords: $leakedPasswords
                    - Family Security Status: $familyStatus
                    - Last URL Scan: $lastScanResult
                    
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
        }}
}