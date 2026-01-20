package com.example.securityangel.ui.vault

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.R
import com.example.securityangel.utils.SecurityConstants
import com.example.securityangel.data.models.PasswordAccount
import com.example.securityangel.data.repo.SecurityLogger
import com.example.securityangel.data.repo.SecurityRepository
import com.example.securityangel.databinding.ActivityPasswordVaultBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.BreachCheckUtil
import com.example.securityangel.utils.EncryptionUtil
import com.example.securityangel.utils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.mutableListOf

class PasswordVaultActivity : BaseActivity() {

    private lateinit var binding: ActivityPasswordVaultBinding
    private lateinit var adapter: PasswordVaultAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var allAccounts = mutableListOf<PasswordAccount>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordVaultBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        setToolbarVisibility(false)

        if (userId == null) {
            toast("Error: User not logged in")
            finish()
            return
        }

        setupRecyclerView()
        setupSearch()
        buttonHandler()
        listenToPasswords()
    }

    override fun buttonHandler() {
        binding.fabAddPassword.setOnClickListener {
            showAddOrEditDialog(null)
        }

        val btnRef = binding.icRefresh
        btnRef.isFocusable = true
        btnRef.isClickable = true
        btnRef.setOnClickListener { scanVaultForLeaks() }

        val btnDrw = binding.icBack
        btnDrw.isFocusable = true
        btnDrw.isClickable = true
        btnDrw.setOnClickListener { openDrawer() }

    }

    private fun setupRecyclerView() {
        adapter = PasswordVaultAdapter(mutableListOf()) { account, action ->
            if (action == "options") {
                showOptionsDialog(account)
            }
        }
        binding.rvPasswords.layoutManager = LinearLayoutManager(this)
        binding.rvPasswords.adapter = adapter
    }

    private fun listenToPasswords() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("users").document(userId).collection("vault")
            .addSnapshotListener { value, error ->
                if (error != null || value == null) {
                    return@addSnapshotListener
                }

                val tempAccounts = mutableListOf<PasswordAccount>()

                for (doc in value) {
                    val encryptedPass = doc.getString("password") ?: ""
                    val encryptedEmail = doc.getString("email") ?: ""

                    val decryptedPass = EncryptionUtil.decrypt(encryptedPass)
                    val decryptedEmail = EncryptionUtil.decrypt(encryptedEmail)

                    val account = PasswordAccount(
                        id = doc.id,
                        siteName = doc.getString("siteName") ?: "",
                        email = decryptedEmail,
                        domain = doc.getString("domain") ?: "",
                        password = decryptedPass,
                        isLeaked = false
                    )
                    tempAccounts.add(account)
                }

                allAccounts = tempAccounts
                adapter.updateList(allAccounts)

                if (allAccounts.isNotEmpty()) {
                    scanVaultForLeaks()
                }
            }
    }

    private fun showAddOrEditDialog(accountToEdit: PasswordAccount? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null)

        val etName = dialogView.findViewById<EditText>(R.id.etSiteName)
        val etDomain = dialogView.findViewById<EditText>(R.id.etDomain)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPass = dialogView.findViewById<EditText>(R.id.etPassword)

        if (accountToEdit != null) {
            etName.setText(accountToEdit.siteName)
            etDomain.setText(accountToEdit.domain)
            etEmail.setText(accountToEdit.email)
            etPass.setText(accountToEdit.password)
        }

        val title = if (accountToEdit == null) "Add Password" else "Edit Password"

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
            val name = etName.text.toString()
            val domain = etDomain.text.toString()
            val email = etEmail.text.toString()
            val rawPass = etPass.text.toString()

            if (name.isNotEmpty() && rawPass.isNotEmpty()) {
                val encryptedPass = EncryptionUtil.encrypt(rawPass)
                val encryptedEmail = EncryptionUtil.encrypt(email)

                val dataMap = hashMapOf(
                    "searchKey" to name.lowercase(),
                    "siteName" to name,
                    "domain" to domain,
                    "email" to encryptedEmail,
                    "password" to encryptedPass
                )

                if (accountToEdit == null) {
                    firestore.collection("users").document(userId!!).collection("vault")
                        .add(dataMap)
                } else {
                    firestore.collection("users").document(userId!!).collection("vault")
                        .document(accountToEdit.id)
                        .update(dataMap as Map<String, Any>)
                }
            }
        }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOptionsDialog(account: PasswordAccount) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(this)
            .setTitle("Options for ${account.siteName}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddOrEditDialog(account) // Edit
                    1 -> deletePassword(account)      // Delete
                }
            }
            .show()
    }

    private fun deletePassword(account: PasswordAccount) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Password")
            .setMessage("Are you sure you want to delete ${account.siteName}?")
            .setPositiveButton("Delete") { _, _ ->
                firestore.collection("users").document(userId!!).collection("vault")
                    .document(account.id)
                    .delete()
                    .addOnSuccessListener {
                        toast("Deleted successfully")
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()

                if (query.isEmpty()) {
                    adapter.updateList(allAccounts)
                    return
                }

                firestore.collection("users").document(userId!!).collection("vault")
                    .orderBy("searchKey")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .get()
                    .addOnSuccessListener { documents ->
                        val searchResults = mutableListOf<PasswordAccount>()

                        for (doc in documents) {
                            val encryptedPass = doc.getString("password") ?: ""
                            val encryptedEmail = doc.getString("email") ?: ""
                            val decryptedPass = EncryptionUtil.decrypt(encryptedPass)
                            val decryptedEmail = EncryptionUtil.decrypt(encryptedEmail)

                            val account = PasswordAccount(
                                id = doc.id,
                                siteName = doc.getString("siteName") ?: "",
                                searchKey = doc.getString("searchKey") ?: "",
                                email = decryptedEmail,
                                domain = doc.getString("domain") ?: "",
                                password = decryptedPass,
                                isLeaked = doc.getBoolean("isLeaked") ?: false
                            )
                            searchResults.add(account)
                        }
                        adapter.updateList(searchResults)
                    }
            }
        })
    }


    private fun scanVaultForLeaks() {
        toast("Starting vault scan...")

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        var leaksFound = 0
        var checkedCount = 0

        if (allAccounts.isEmpty()) {
            toast("Vault is empty")
            return
        }

        for (account in allAccounts) {

            BreachCheckUtil.checkPassword(account.password) { isLeaked ->
                checkedCount++

                if (isLeaked) {
                    leaksFound++
                    onPasswordLeakDetected()
                    saveToFirebase(account)
                    SecurityLogger.logEvent(
                        SecurityLogger.TYPE_LEAK_FOUND,
                        "Password leak detected for site: ${account.siteName}"
                    )
                    runOnUiThread {
                         account.isLeaked = true
                         adapter.notifyDataSetChanged()
                    }
                }

                if (checkedCount == allAccounts.size) {
                    runOnUiThread {
                        if (leaksFound > 0) {
                            toast("⚠️ found $leaksFound compromised passwords in your vault")
                        } else {
                            toast("Scan complete. No leaks found! ✅")
                            onPasswordFixed()
                        }
                    }
                }
            }
        }
    }

    fun onPasswordLeakDetected() {
        val myUserId = FirebaseAuth.getInstance().currentUser!!.uid
        SecurityRepository.reportRisk(myUserId, SecurityConstants.RISK_PASSWORD_LEAK)
    }

    fun onPasswordFixed() {
        val myUserId = FirebaseAuth.getInstance().currentUser!!.uid
        SecurityRepository.resolveRisk(myUserId, SecurityConstants.RISK_PASSWORD_LEAK)
    }

    private fun saveToFirebase(account : PasswordAccount){
        firestore.collection("users").document(userId!!)
            .collection("vault").document(account.id)
            .update("isLeaked", true)

    }


}
