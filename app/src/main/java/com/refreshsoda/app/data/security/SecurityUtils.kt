package com.refreshsoda.app.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password / PIN hashing.
 *
 * New format : pbkdf2$<iterations>$<saltBase64>$<hashBase64>   (random salt per user)
 * Legacy     : 64-char hex SHA-256 with one fixed app salt     (verified, then upgraded on login)
 * Legacy     : plain text                                       (verified, then upgraded on login)
 *
 * IMPORTANT: a stored value is never accepted as the password itself. Earlier versions did
 * `rawInput == stored`, which let anyone who could read the stored hash log in by typing the hash.
 */
object SecurityUtils {
    private const val LEGACY_SALT = "refresh_soda_shop_2026_secure"
    private const val LEGACY_PIN_SALT = "refresh_pin_salt"
    private const val PREFIX = "pbkdf2"
    private const val SEP = "\$"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    private val random = SecureRandom()

    fun hashPassword(password: String): String = pbkdf2Encode(password)

    fun verifyPassword(rawInput: String, stored: String): Boolean = verify(rawInput, stored, LEGACY_SALT)

    /** True when the stored value is not yet in the current PBKDF2 format and should be re-hashed after a successful login. */
    fun needsUpgrade(stored: String): Boolean = !stored.startsWith(PREFIX + SEP)

    fun hashPin(pin: String): String = pbkdf2Encode(pin)

    fun verifyPin(pinInput: String, storedPinHash: String): Boolean = verify(pinInput, storedPinHash, LEGACY_PIN_SALT)

    // ---------------------------------------------------------------------------------------------

    private fun verify(rawInput: String, stored: String, legacySalt: String): Boolean {
        if (stored.isBlank()) return false
        return when {
            stored.startsWith(PREFIX + SEP) -> verifyPbkdf2(rawInput, stored)
            isLegacySha256(stored) -> constantTimeEquals(
                legacySha256(rawInput, legacySalt),
                stored.lowercase()
            )
            // Oldest accounts stored the password in clear text.
            else -> constantTimeEquals(rawInput, stored)
        }
    }

    private fun pbkdf2Encode(secret: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2(secret, salt, ITERATIONS)
        val encoder = Base64.getEncoder()
        return listOf(PREFIX, ITERATIONS.toString(), encoder.encodeToString(salt), encoder.encodeToString(hash)).joinToString(SEP)
    }

    private fun verifyPbkdf2(rawInput: String, stored: String): Boolean {
        return try {
            val parts = stored.split(SEP)
            if (parts.size != 4) return false
            val iterations = parts[1].toInt()
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(parts[2])
            val expected = decoder.decode(parts[3])
            val actual = pbkdf2(rawInput, salt, iterations)
            MessageDigest.isEqual(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun isLegacySha256(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    private fun legacySha256(secret: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$salt:$secret".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
}
