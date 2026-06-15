package com.blockads.app.core.data.dns

import java.nio.ByteBuffer
import java.nio.ByteOrder

private val DOMAIN_REGEX = Regex("^[a-z0-9]([a-z0-9._-]{0,251}[a-z0-9])?$")

sealed class ParseResult {
    data class Success(
        val domain: String,
        val txId: Short,
        val rawQuery: ByteArray,
    ) : ParseResult()

    data object Malformed : ParseResult()
}

object DnsPacketParser {
    fun parseQuery(udpPayload: ByteArray): ParseResult {
        if (udpPayload.size < 12) return ParseResult.Malformed
        
        return try {
            val buf = ByteBuffer.wrap(udpPayload).order(ByteOrder.BIG_ENDIAN)
            val txId = buf.short

            val flags = buf.short.toInt() and 0xFFFF
            // QR bit (bit 15) must be 0 for a query
            if ((flags and 0x8000) != 0) return ParseResult.Malformed

            val qdCount = buf.short.toInt() and 0xFFFF
            if (qdCount < 1) return ParseResult.Malformed
            
            // Skip anCount, nsCount, arCount (must be 12 bytes total header)
            buf.position(12)

            val domain = buildString {
                var iterations = 0
                while (iterations++ < 128) {
                    if (!buf.hasRemaining()) return ParseResult.Malformed
                    val labelLen = buf.get().toInt() and 0xFF
                    
                    if (labelLen == 0) break
                    
                    // DNS compression (pointers) are not expected in standard queries
                    // but we check for them to be safe (starts with 0xC0)
                    if ((labelLen and 0xC0) != 0) return ParseResult.Malformed
                    
                    if (buf.remaining() < labelLen) return ParseResult.Malformed
                    
                    val label = ByteArray(labelLen)
                    buf.get(label)
                    append(String(label, Charsets.US_ASCII))
                    append('.')
                }
            }.trimEnd('.')

            if (domain.isEmpty()) return ParseResult.Malformed
            val lowerDomain = domain.lowercase()
            if (!DOMAIN_REGEX.matches(lowerDomain)) return ParseResult.Malformed

            ParseResult.Success(lowerDomain, txId, udpPayload)
        } catch (e: Exception) {
            ParseResult.Malformed
        }
    }
}
