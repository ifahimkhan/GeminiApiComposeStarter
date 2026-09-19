package com.fahim.geminiApiComposeStarter.security

/** Encrypts / decrypts small secrets. An interface so the real Keystore can be swapped in tests. */
interface SecretCipher {
    /** Returns a Base64 payload (IV + ciphertext + GCM tag). */
    fun encrypt(plainText: String): String

    /** Reverses [encrypt]. Throws if the payload is corrupt or the key is gone. */
    fun decrypt(payload: String): String
}
