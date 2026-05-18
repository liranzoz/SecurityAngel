package com.example.securityangel.ui.vault

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securityangel.R
import com.example.securityangel.data.models.PasswordAccount
import com.example.securityangel.data.repo.SecurityLogger
import com.example.securityangel.data.repo.SecurityRepository
import com.example.securityangel.databinding.ActivityPasswordVaultBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.BiometricKeyStoreHelper
import com.example.securityangel.utils.BiometricManager
import com.example.securityangel.utils.BreachCheckUtil
import com.example.securityangel.utils.VaultSessionManager
import com.example.securityangel.utils.SecurityConstants
import com.example.securityangel.utils.VaultCryptoManager
import com.example.securityangel.utils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordVaultActivity : BaseActivity() {

    private lateinit var binding: ActivityPasswordVaultBinding
    private lateinit var adapter: PasswordVaultAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var allAccounts = mutableListOf<PasswordAccount>()

    private var masterPin = ""
    private var vaultSalt = ""

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
        fetchSaltThenUnlock()
    }

    private fun fetchSaltThenUnlock() {

        if (VaultSessionManager.isValid) {
            masterPin = VaultSessionManager.masterPin
            vaultSalt = VaultSessionManager.vaultSalt
            listenToPasswords()
            return
        }

        firestore.collection("users").document(userId!!)
            .get()
            .addOnSuccessListener { doc ->
                val salt = doc.getString("vaultSalt")
                if (salt.isNullOrEmpty()) {
                    toast("Vault not configured. Please open the dashboard once and try again.")
                    finish()
                    return@addOnSuccessListener
                }
                vaultSalt = salt
                tryBiometricUnlock()
            }
            .addOnFailureListener {
                toast("Could not load vault configuration. Check your connection.")
                finish()
            }
    }

    private fun tryBiometricUnlock() {
        val biometricEnabled = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("biometric_enabled", false)

        if (biometricEnabled
            && BiometricManager.isBiometricAvailable(this)
            && BiometricKeyStoreHelper.hasPinStored(this)
        ) {
            BiometricKeyStoreHelper.showDecryptPrompt(
                activity = this,
                title    = "Unlock Vault",
                subtitle = "Authenticate to open your password vault",
                onSuccess = { pin -> unlockWithPin(pin, fromBiometric = true) },
                onFailure  = { showPinDialog() }
            )
        } else {
            showPinDialog()
        }
    }

    private fun showPinDialog() {
        val pinInput = EditText(this).apply {
            hint      = "Enter your Vault PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(64, 32, 64, 16)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Unlock Vault")
            .setMessage("Enter your master PIN to decrypt your passwords.")
            .setView(pinInput)
            .setCancelable(false)
            .setPositiveButton("Unlock") { _, _ ->
                val pin = pinInput.text.toString()
                if (pin.isEmpty()) {
                    toast("PIN cannot be empty")
                    finish()
                    return@setPositiveButton
                }
                unlockWithPin(pin, fromBiometric = false)
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun unlockWithPin(pin: String, fromBiometric: Boolean) {
        masterPin = pin
        VaultSessionManager.save(pin, vaultSalt)
        if (!fromBiometric) offerBiometricPinStorage(pin)
        listenToPasswords()
    }

    private fun offerBiometricPinStorage(pin: String) {
        val biometricEnabled = getSharedPreferences("AppScanSettings", MODE_PRIVATE)
            .getBoolean("biometric_enabled", false)

        if (!biometricEnabled
            || !BiometricManager.isBiometricAvailable(this)
            || BiometricKeyStoreHelper.hasPinStored(this)
        ) return

        BiometricKeyStoreHelper.showEncryptPrompt(
            activity  = this,
            pin       = pin,
            onSuccess = { toast("Vault PIN protected — future unlocks will use biometrics") },
            onCancel  = {}
        )
    }

    override fun buttonHandler() {
        binding.fabAddPassword.setOnClickListener {
            if (masterPin.isNotEmpty() && vaultSalt.isNotEmpty()) showAddOrEditDialog(null)
        }

        binding.icRefresh.apply {
            isFocusable = true
            isClickable  = true
            setOnClickListener { scanVaultForLeaks() }
        }

        binding.icBack.apply {
            isFocusable = true
            isClickable  = true
            setOnClickListener { openDrawer() }
        }
    }

    private fun setupRecyclerView() {
        adapter = PasswordVaultAdapter(mutableListOf()) { account, action ->
            if (action == "options") showOptionsDialog(account)
        }
        binding.rvPasswords.layoutManager = LinearLayoutManager(this)
        binding.rvPasswords.adapter = adapter
    }

    private fun listenToPasswords() {
        firestore.collection("users").document(userId!!).collection("vault")
            .addSnapshotListener { value, error ->
                if (error != null || value == null) return@addSnapshotListener

                lifecycleScope.launch(Dispatchers.IO) {
                    val tempAccounts = mutableListOf<PasswordAccount>()
                    var wrongPin = false

                    for (doc in value) {
                        val encryptedPass  = doc.getString("password") ?: ""
                        val encryptedEmail = doc.getString("email")    ?: ""

                        val decryptedPass  = VaultCryptoManager.decrypt(encryptedPass,  masterPin, vaultSalt)
                        val decryptedEmail = VaultCryptoManager.decrypt(encryptedEmail, masterPin, vaultSalt)

                        if (encryptedPass.isNotEmpty() && decryptedPass.isEmpty()) {
                            wrongPin = true
                            break
                        }

                        tempAccounts.add(
                            PasswordAccount(
                                id       = doc.id,
                                siteName = doc.getString("siteName") ?: "",
                                email    = decryptedEmail,
                                domain   = doc.getString("domain")   ?: "",
                                password = decryptedPass,
                                isLeaked = false
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (wrongPin) {
                            toast("Incorrect PIN — please reopen the vault and try again.")
                            finish()
                            return@withContext
                        }
                        allAccounts = tempAccounts
                        adapter.updateList(allAccounts)
                        if (allAccounts.isNotEmpty()) scanVaultForLeaks()
                    }
                }
            }
    }

    private fun showAddOrEditDialog(accountToEdit: PasswordAccount? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null)

        val etName   = dialogView.findViewById<EditText>(R.id.etSiteName)
        val etDomain = dialogView.findViewById<EditText>(R.id.etDomain)
        val etEmail  = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPass   = dialogView.findViewById<EditText>(R.id.etPassword)

        if (accountToEdit != null) {
            etName.setText(accountToEdit.siteName)
            etDomain.setText(accountToEdit.domain)
            etEmail.setText(accountToEdit.email)
            etPass.setText(accountToEdit.password)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (accountToEdit == null) "Add Password" else "Edit Password")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name    = etName.text.toString()
                val domain  = etDomain.text.toString()
                val email   = etEmail.text.toString()
                val rawPass = etPass.text.toString()

                if (name.isNotEmpty() && rawPass.isNotEmpty()) {
                    val encryptedPass  = VaultCryptoManager.encrypt(rawPass, masterPin, vaultSalt)
                    val encryptedEmail = VaultCryptoManager.encrypt(email,   masterPin, vaultSalt)

                    val dataMap = hashMapOf(
                        "searchKey" to name.lowercase(),
                        "siteName"  to name,
                        "domain"    to domain,
                        "email"     to encryptedEmail,
                        "password"  to encryptedPass
                    )

                    if (accountToEdit == null) {
                        firestore.collection("users").document(userId!!).collection("vault").add(dataMap)
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Options for ${account.siteName}")
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showAddOrEditDialog(account)
                    1 -> deletePassword(account)
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
                    .addOnSuccessListener { toast("Deleted successfully") }
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

                if (masterPin.isEmpty() || vaultSalt.isEmpty()) return

                firestore.collection("users").document(userId!!).collection("vault")
                    .orderBy("searchKey")
                    .startAt(query)
                    .endAt(query + "")
                    .get()
                    .addOnSuccessListener { documents ->

                        val results = documents.map { doc ->
                            PasswordAccount(
                                id        = doc.id,
                                siteName  = doc.getString("siteName")  ?: "",
                                searchKey = doc.getString("searchKey") ?: "",
                                email     = VaultCryptoManager.decrypt(doc.getString("email")    ?: "", masterPin, vaultSalt),
                                domain    = doc.getString("domain")    ?: "",
                                password  = VaultCryptoManager.decrypt(doc.getString("password") ?: "", masterPin, vaultSalt),
                                isLeaked  = doc.getBoolean("isLeaked") ?: false
                            )
                        }
                        adapter.updateList(results)
                    }
            }
        })
    }

    private fun scanVaultForLeaks() {
        toast("Starting vault scan...")

        if (allAccounts.isEmpty()) {
            toast("Vault is empty")
            return
        }

        var leaksFound   = 0
        var checkedCount = 0

        for (account in allAccounts) {
            BreachCheckUtil.checkPassword(account.password) { isLeaked ->
                checkedCount++

                if (isLeaked) {
                    leaksFound++
                    onPasswordLeakDetected()
                    saveLeakFlagToFirebase(account)
                    SecurityLogger.logEvent(
                        SecurityLogger.TYPE_LEAK_FOUND,
                        "Password Leak Detected",
                        "Password for ${account.siteName} was found in a breach."
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

    private fun saveLeakFlagToFirebase(account: PasswordAccount) {
        firestore.collection("users").document(userId!!)
            .collection("vault").document(account.id)
            .update("isLeaked", true)
    }
}
