package com.fahim.geminiApiComposeStarter.data.security

/** Symmetric encryption; the output of [encrypt] is self-contained (it carries its own IV). */
interface SecretCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}
