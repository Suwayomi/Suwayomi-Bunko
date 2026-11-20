/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.prefs

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedPreference(
    private val raw: Preference<String>,
) : Preference<String> {
    override fun key() = raw.key()

    override fun get(): String = decrypt(raw.get())

    override fun set(value: String) = raw.set(encrypt(value))

    override fun isSet() = raw.isSet()

    override fun delete() = raw.delete()

    override fun defaultValue() = ""

    override fun changes(): Flow<String> = raw.changes().map(::decrypt)

    override fun stateIn(scope: CoroutineScope): StateFlow<String> = changes().stateIn(scope, SharingStarted.Eagerly, get())

    private fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        return decryptOrNull(stored) ?: "".also { raw.delete() }
    }
}

private const val KEY_ALIAS = "bunko_secrets"
private const val IV_BYTES = 12
private const val TAG_BITS = 128

private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        generateKey()
    }
}

internal fun encrypt(plain: String): String {
    if (plain.isEmpty()) return ""
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
    return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray()), Base64.NO_WRAP)
}

internal fun decryptOrNull(stored: String): String? =
    runCatching {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES)) }
        String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
    }.getOrNull()
