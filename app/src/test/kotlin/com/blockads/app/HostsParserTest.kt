package com.blockads.app

import com.blockads.app.core.data.blocklist.HostsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HostsParserTest {
    private fun parse(vararg lines: String): List<String> = HostsParser.parseLines(lines.asSequence()).toList()

    @Test
    fun `parses standard hosts format`() {
        val result = parse("0.0.0.0 ads.example.com")
        assertEquals(listOf("ads.example.com"), result)
    }

    @Test
    fun `skips comment lines`() {
        val result = parse("# This is a comment", "0.0.0.0 ads.example.com")
        assertEquals(listOf("ads.example.com"), result)
    }

    @Test
    fun `skips blank lines`() {
        val result = parse("", "   ", "0.0.0.0 valid.com")
        assertEquals(listOf("valid.com"), result)
    }

    @Test
    fun `strips inline comments`() {
        val result = parse("0.0.0.0 ads.example.com # inline comment")
        assertEquals(listOf("ads.example.com"), result)
    }

    @Test
    fun `normalises domain to lowercase`() {
        val result = parse("0.0.0.0 ADS.EXAMPLE.COM")
        assertEquals(listOf("ads.example.com"), result)
    }

    @Test
    fun `rejects localhost`() {
        val result = parse("0.0.0.0 localhost")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `rejects broadcasthost`() {
        val result = parse("255.255.255.255 broadcasthost")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `rejects domain longer than 253 characters`() {
        val longDomain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(64)
        val result = parse("0.0.0.0 $longDomain")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `accepts plain domain without IP prefix`() {
        val result = parse("ads.example.com")
        assertEquals(listOf("ads.example.com"), result)
    }

    @Test
    fun `rejects non-printable characters`() {
        val result = parse("0.0.0.0 ads\u0000.example.com")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles large input lazily`() {
        val manyLines = generateSequence { "0.0.0.0 ads.example.com" }.take(100_000)
        val count = HostsParser.parseLines(manyLines).count()
        assertEquals(100_000, count)
    }
}
