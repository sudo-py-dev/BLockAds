package com.blockads.app.core.vpn

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class IPv6PacketParsingTest {
    @Test
    fun testIPv6WithExtensionHeaders() {
        // Construct a mock IPv6 packet with extension headers
        // Fixed header (40 bytes) + Hop-by-Hop (8 bytes) + Dest Options (8 bytes) + UDP (8 bytes)
        val packet = ByteArray(100)
        val buf = ByteBuffer.wrap(packet)

        // IPv6 Fixed Header
        buf.put(0, (6 shl 4).toByte()) // Version 6
        buf.put(6, 0.toByte()) // Next Header: Hop-by-Hop (0)

        // Hop-by-Hop Header (offset 40)
        buf.put(40, 60.toByte()) // Next Header: Destination Options (60)
        buf.put(41, 0.toByte()) // Length: (0+1)*8 = 8 bytes

        // Destination Options Header (offset 48)
        buf.put(48, 17.toByte()) // Next Header: UDP (17)
        buf.put(49, 0.toByte()) // Length: (0+1)*8 = 8 bytes

        // UDP Header (offset 56)
        buf.putShort(56, 1234.toShort()) // Source Port
        buf.putShort(58, 53.toShort()) // Dest Port (DNS)

        // Simulated parser logic from BlockAdsVpnService
        val len = 100
        val version = (packet[0].toInt() and 0xF0) shr 4
        assertEquals(6, version)

        var nextHeader = packet[6].toInt() and 0xFF
        var offset = 40

        while (offset < len) {
            if (nextHeader == 17) break

            if (nextHeader == 0 || nextHeader == 43 || nextHeader == 60) {
                val extLen = ((packet[offset + 1].toInt() and 0xFF) + 1) * 8
                nextHeader = packet[offset].toInt() and 0xFF
                offset += extLen
            } else if (nextHeader == 44) {
                nextHeader = packet[offset].toInt() and 0xFF
                offset += 8
            } else {
                break
            }
        }

        assertEquals(17, nextHeader) // Protocol should be UDP
        assertEquals(56, offset) // Offset should point to UDP header

        val destPort = ((packet[offset + 2].toInt() and 0xFF) shl 8) or (packet[offset + 3].toInt() and 0xFF)
        assertEquals(53, destPort)
    }
}
