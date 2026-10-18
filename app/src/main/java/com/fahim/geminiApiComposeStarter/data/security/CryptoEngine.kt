package com.fahim.geminiApiComposeStarter.data.security

/**
 * Interface abstracting cryptographic operations to facilitate unit testing on the JVM
 * while using Android Keystore at runtime on Android.
 */
interface CryptoEngine {
    fun encrypt(plainText: String): EncryptedData
    fun decrypt(encryptedData: EncryptedData): String
}
