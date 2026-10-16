package com.scrapchef.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenEncryptorTest {

    @Test
    fun noOpTokenEncryptor_passesThroughPlaintext() {
        val encryptor = NoOpTokenEncryptor()
        val plain = "test_puter_token_12345"
        val encrypted = encryptor.encrypt(plain)
        val decrypted = encryptor.decrypt(encrypted)

        assertEquals(plain, encrypted)
        assertEquals(plain, decrypted)
    }

    @Test
    fun noOpTokenEncryptor_handlesEmptyAndBlank() {
        val encryptor = NoOpTokenEncryptor()
        assertEquals("", encryptor.encrypt(""))
        assertEquals("", encryptor.decrypt(""))
    }

    @Test
    fun androidKeystoreEncryptor_gracefullyHandlesInvalidCipher() {
        val encryptor = AndroidKeystoreTokenEncryptor()
        // On JVM host tests without KeyStore provider, should return input safely rather than crash
        val invalid = "plain_token_string"
        val decrypted = encryptor.decrypt(invalid)
        assertEquals(invalid, decrypted)
    }
}
