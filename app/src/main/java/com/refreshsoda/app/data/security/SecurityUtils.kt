package com.refreshsoda.app.data.security

import java.security.MessageDigest

object SecurityUtils {
    private const val DEFAULT_SALT = "refresh_soda_shop_2026_secure"

    fun hashPassword(password: String, salt: String = DEFAULT_SALT): String {
        val input = "$salt:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(rawInput: String, storedHashOrPlain: String, salt: String = DEFAULT_SALT): Boolean {
        if (rawInput == storedHashOrPlain) return true // Legacy fallback
        val computedHash = hashPassword(rawInput, salt)
        return computedHash.equals(storedHashOrPlain, ignoreCase = true)
    }

    fun hashPin(pin: String): String {
        return hashPassword(pin, "refresh_pin_salt")
    }

    fun verifyPin(pinInput: String, storedPinHash: String): Boolean {
        if (pinInput == storedPinHash) return true
        return hashPin(pinInput).equals(storedPinHash, ignoreCase = true)
    }
}

