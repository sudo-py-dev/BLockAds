package com.adsblock.vpn.service

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class IpUtilTest {
    @Test
    fun testBuildUdpIpPacket_ValidInput_ConstructsCorrectPacket() {
        val sourceIp = 0xC0A80001.toInt() // 192.168.0.1
        val destIp = 0x08080808 // 8.8.8.8
        val sourcePort: Short = 12345
        val destPort: Short = 53
        val payload = "hello".toByteArray()

        val packet =
            IpUtil.buildUdpIpPacket(
                sourceIp = sourceIp,
                destIp = destIp,
                sourcePort = sourcePort,
                destPort = destPort,
                payload = payload,
            )

        // Minimum length = 20 (IP) + 8 (UDP) + 5 (Payload) = 33
        assertEquals(33, packet.size)

        val buffer = ByteBuffer.wrap(packet)

        // Verify IPv4 Header
        val versionAndIhl = buffer.get(0).toInt()
        assertEquals(4, versionAndIhl shr 4) // Version 4
        assertEquals(5, versionAndIhl and 0x0F) // IHL 5 (20 bytes)

        assertEquals(33, buffer.getShort(2).toInt()) // Total Length
        assertEquals(17, buffer.get(9).toInt()) // Protocol UDP

        assertEquals(sourceIp, buffer.getInt(12))
        assertEquals(destIp, buffer.getInt(16))

        // Verify UDP Header
        buffer.position(20)
        assertEquals(sourcePort, buffer.getShort())
        assertEquals(destPort, buffer.getShort())
        assertEquals(13, buffer.getShort().toInt()) // UDP Length: 8 + 5 = 13

        // Verify Payload
        buffer.position(28)
        val extractedPayload = ByteArray(5)
        buffer.get(extractedPayload)

        assertArrayEquals(payload, extractedPayload)
    }
}
