package com.adsblock.vpn.service

import java.nio.ByteBuffer

object IpUtil {
    fun buildUdpIpPacket(
        sourceIp: Int,
        destIp: Int,
        sourcePort: Short,
        destPort: Short,
        payload: ByteArray,
    ): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val buffer = ByteBuffer.allocate(totalLength)

        // IPv4 Header (20 bytes)
        buffer.put((0x45).toByte()) // Version 4, IHL 5
        buffer.put(0.toByte()) // TOS
        buffer.putShort(totalLength.toShort()) // Total Length
        buffer.putShort(0.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags & Fragment Offset (Don't Fragment)
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol (UDP)
        buffer.putShort(0.toShort()) // Initial Header Checksum
        buffer.putInt(sourceIp)
        buffer.putInt(destIp)

        // Calculate IP Checksum
        val ipChecksum = computeChecksum(buffer.array(), 0, 20)
        buffer.putShort(10, ipChecksum)

        // UDP Header (8 bytes)
        val udpOffset = 20
        buffer.position(udpOffset)
        buffer.putShort(sourcePort)
        buffer.putShort(destPort)
        buffer.putShort((8 + payload.size).toShort()) // UDP Length
        buffer.putShort(0.toShort()) // UDP Checksum (0 means optional in IPv4)

        // Payload
        buffer.put(payload)

        return buffer.array()
    }

    private fun computeChecksum(
        data: ByteArray,
        offset: Int,
        length: Int,
    ): Short {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }
}
