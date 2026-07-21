package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's personal ("Personalise AI") OpenAI key encrypted at rest.
 *
 * The key is a credential, so it must never sit in plaintext on disk. We encrypt
 * it with AES-256/GCM using a key held in the Android Keystore (`AndroidKeyStore`),
 * whose master key material is hardware-backed where available and never leaves
 * the device. The ciphertext lives in its own SharedPreferences file
 * ([PREFS_NAME]) which is excluded from Auto Backup and device transfer (see
 * res/xml/data_extraction_rules.xml) — a restored copy could not be decrypted
 * elsewhere anyway, since the Keystore key is non-exportable.
 *
 * This intentionally does not use androidx.security:security-crypto
 * (EncryptedSharedPreferences), which Google has deprecated.
 */
internal class SecureApiKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the stored key, or null if none is set or the ciphertext can't be decrypted. */
    fun get(): String? {
        val stored = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        return try {
            val (iv, ciphertext) = decode(stored)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            String(cipher.doFinal(ciphertext), Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            // Key invalidated / corrupt entry: drop it rather than crash. The user
            // can re-enter their key.
            clear()
            null
        }
    }

    fun set(value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit { putString(KEY_CIPHERTEXT, encode(cipher.iv, ciphertext)) }
    }

    fun clear() {
        prefs.edit { remove(KEY_CIPHERTEXT) }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** Encodes iv and ciphertext as "base64(iv):base64(ciphertext)". */
    private fun encode(iv: ByteArray, ciphertext: ByteArray): String =
        "${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(ciphertext, Base64.NO_WRAP)}"

    private fun decode(stored: String): Pair<ByteArray, ByteArray> {
        val parts = stored.split(":", limit = 2)
        require(parts.size == 2) { "Malformed ciphertext" }
        return Base64.decode(parts[0], Base64.NO_WRAP) to Base64.decode(parts[1], Base64.NO_WRAP)
    }

    companion object {
        private const val PREFS_NAME = "secure_settings"
        private const val KEY_CIPHERTEXT = "api_key_override_enc"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "dailymacros_api_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
