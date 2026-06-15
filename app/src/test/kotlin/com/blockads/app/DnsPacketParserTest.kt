package com.blockads.app

import com.blockads.app.core.data.dns.DnsPacketParser
import com.blockads.app.core.data.dns.ParseResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DnsPacketParserTest {
    private fun buildDnsQuery(
        domain: String,
        txId: Short = 1234,
    ): ByteArray {
        val labels = domain.split('.')
        val qname =
            buildList<Byte> {
                for (label in labels) {
                    add(label.length.toByte())
                    addAll(label.map { it.code.toByte() })
                }
                add(0)
            }
        val header =
            byteArrayOf(
                (txId.toInt() shr 8).toByte(), txId.toByte(),
                0x01, 0x00,
                0x00, 0x01,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00,
            )
        val question = qname.toByteArray() + byteArrayOf(0x00, 0x01, 0x00, 0x01)
        return header + question
    }

    @Test
    fun `valid A query parses correctly`() {
        val packet = buildDnsQuery("example.com")
        val result = DnsPacketParser.parseQuery(packet)
        assertTrue(result is ParseResult.Success)
        assertEquals("example.com", (result as ParseResult.Success).domain)
    }

    @Test
    fun `packet too short returns Malformed`() {
        val result = DnsPacketParser.parseQuery(ByteArray(11))
        assertEquals(ParseResult.Malformed, result)
    }

    @Test
    fun `null byte in qname returns Malformed`() {
        val packet = buildDnsQuery("example.com").toMutableList()
        packet[13] = 0x00
        val result = DnsPacketParser.parseQuery(packet.toByteArray())
        assertEquals(ParseResult.Malformed, result)
    }

    @Test
    fun `label length exceeds 63 returns Malformed`() {
        val base = buildDnsQuery("example.com").toMutableList()
        base[12] = 64
        val result = DnsPacketParser.parseQuery(base.toByteArray())
        assertEquals(ParseResult.Malformed, result)
    }

    @Test
    fun `domain with invalid characters returns Malformed`() {
        val packet = buildDnsQuery("ex@mple.com")
        val result = DnsPacketParser.parseQuery(packet)
        assertEquals(ParseResult.Malformed, result)
    }

    @Test
    fun `uppercase domain is lowercased`() {
        val packet = buildDnsQuery("EXAMPLE.COM")
        val result = DnsPacketParser.parseQuery(packet)
        assertTrue(result is ParseResult.Success)
        assertEquals("example.com", (result as ParseResult.Success).domain)
    }

    @Test
    fun `empty packet returns Malformed`() {
        assertEquals(ParseResult.Malformed, DnsPacketParser.parseQuery(ByteArray(0)))
    }
}
