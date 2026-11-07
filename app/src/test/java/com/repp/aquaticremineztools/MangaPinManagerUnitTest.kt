package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.core.security.MangaPinManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MangaPinManagerUnitTest {

    @Test
    fun testPbkdf2HashConsistency() {
        val pin = "123456"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val hash1 = MangaPinManager.hashPin(pin, salt)
        val hash2 = MangaPinManager.hashPin(pin, salt)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testDifferentPinsProduceDifferentHashes() {
        val pin1 = "123456"
        val pin2 = "654321"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val hash1 = MangaPinManager.hashPin(pin1, salt)
        val hash2 = MangaPinManager.hashPin(pin2, salt)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testDifferentSaltsProduceDifferentHashes() {
        val pin = "123456"
        val salt1 = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val salt2 = byteArrayOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        val hash1 = MangaPinManager.hashPin(pin, salt1)
        val hash2 = MangaPinManager.hashPin(pin, salt2)

        assertNotEquals(hash1, hash2)
    }
}
