package io.yarburart.reiproxy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class EncodingTest {

    @Test
    fun `test decode empty bytes`() {
        assertEquals("", decodeBytes(byteArrayOf()))
    }

    @Test
    fun `test decode standard UTF-8`() {
        val input = "Hello, World!"
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        assertEquals(input, decodeBytes(bytes))
    }

    @Test
    fun `test decode Russian UTF-8`() {
        val input = "Привет, мир!"
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        assertEquals(input, decodeBytes(bytes))
    }

    @Test
    fun `test decode Russian UTF-8 with weak hint`() {
        val input = "Привет, мир!"
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        // Even if hint is ISO-8859-1, it should detect UTF-8
        assertEquals(input, decodeBytes(bytes, StandardCharsets.ISO_8859_1))
    }

    @Test
    fun `test decode Russian Windows-1251 with no hint`() {
        val input = "Текст в 1251"
        val bytes = input.toByteArray(Charset.forName("windows-1251"))
        assertEquals(input, decodeBytes(bytes))
    }

    @Test
    fun `test decode with UTF-8 BOM`() {
        val input = "BOM Test"
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + input.toByteArray(StandardCharsets.UTF_8)
        assertEquals(input, decodeBytes(bytes))
    }

    @Test
    fun `test decode with hint charset`() {
        val input = "Привет!"
        val charset = Charset.forName("windows-1251")
        val bytes = input.toByteArray(charset)
        assertEquals(input, decodeBytes(bytes, charset))
    }

    @Test
    fun `test isPrintableExtended for Cyrillic`() {
        assertTrue('П'.isPrintableExtended())
        assertTrue('я'.isPrintableExtended())
        assertTrue('!'.isPrintableExtended())
        assertTrue(' '.isPrintableExtended())
        assertTrue('\n'.isPrintableExtended())
    }

    @Test
    fun `test fallback to ISO-8859-1 for binary-like data`() {
        val bytes = byteArrayOf(0x80.toByte(), 0xFF.toByte())
        val decoded = decodeBytes(bytes)
        // ISO-8859-1 just treats them as characters
        assertTrue(decoded.isNotEmpty())
    }
}
