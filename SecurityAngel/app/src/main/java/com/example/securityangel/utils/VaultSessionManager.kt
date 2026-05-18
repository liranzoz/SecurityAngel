package com.example.securityangel.utils

object VaultSessionManager {

    private var _masterPin: String = ""
    private var _vaultSalt: String = ""

    val masterPin: String get() = _masterPin
    val vaultSalt: String get() = _vaultSalt

    val isValid: Boolean get() = _masterPin.isNotEmpty() && _vaultSalt.isNotEmpty()

    fun save(pin: String, salt: String) {
        _masterPin = pin
        _vaultSalt = salt
    }

    fun clear() {
        _masterPin = ""
        _vaultSalt = ""
    }
}
