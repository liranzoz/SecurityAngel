package com.example.securityangel.ui.password

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import com.example.securityangel.databinding.ActivityPasswordGeneratorBinding
import com.example.securityangel.ui.base.BaseActivity
import com.example.securityangel.utils.toast

class PasswordGeneratorActivity : BaseActivity() {

    private lateinit var binding: ActivityPasswordGeneratorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordGeneratorBinding.inflate(layoutInflater)
        setContent(binding.root)
        setToolbarIconColor(isDarkBackground = false)
        buttonHandler()
        generatePassword()
    }

    override fun buttonHandler() {
        binding.sliderLength.addOnChangeListener { _, value, _ ->
            binding.tvLengthValue.text = value.toInt().toString()
        }

        binding.btnGenerate.setOnClickListener {
            generatePassword()
        }

        binding.btnCopy.setOnClickListener {
            val password = binding.tvPasswordResult.text.toString()
            copyToClipboard(password)
        }
    }

    private fun generatePassword() {
        val length = binding.sliderLength.value.toInt()
        val includeUpper = binding.cbUppercase.isChecked
        val includeLower = binding.cbLowercase.isChecked
        val includeNumbers = binding.cbNumbers.isChecked
        val includeSymbols = binding.cbSymbols.isChecked

        if (!includeUpper && !includeLower && !includeNumbers && !includeSymbols) {
            toast("Select at least one option")
            return
        }

        val generated = createRandomString(length, includeUpper, includeLower, includeNumbers, includeSymbols)
        binding.tvPasswordResult.text = generated
    }

    private fun createRandomString(length: Int, upper: Boolean, lower: Boolean, nums: Boolean, symbols: Boolean): String {
        val upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowerChars = "abcdefghijklmnopqrstuvwxyz"
        val numberChars = "0123456789"
        val symbolChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        var allowedChars = ""
        if (upper) allowedChars += upperChars
        if (lower) allowedChars += lowerChars
        if (nums) allowedChars += numberChars
        if (symbols) allowedChars += symbolChars

        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Generated Password", text)
        clipboard.setPrimaryClip(clip)
        toast("Password copied!") }
}
