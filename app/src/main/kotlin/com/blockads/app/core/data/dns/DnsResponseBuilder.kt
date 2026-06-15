package com.blockads.app.core.data.dns

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DnsResponseBuilder {
    fun nxdomain(
        txId: Short,
        querySection: ByteArray,
    ): ByteArray {

        val flags: Short = 0x8183.toShort()

        val qSectionSize = querySection.size - 12
        val responseSize = 12 + qSectionSize.coerceAtLeast(0)
        val buf = ByteBuffer.allocate(responseSize).order(ByteOrder.BIG_ENDIAN)

        buf.putShort(txId)
        buf.putShort(flags)
        buf.putShort(1)
        buf.putShort(0)
        buf.putShort(0)
        buf.putShort(0)
        if (qSectionSize > 0) {
            buf.put(querySection, 12, qSectionSize)
        }

        return buf.array()
    }

    fun noError(
        txId: Short,
        querySection: ByteArray,
        answerSection: ByteArray,
    ): ByteArray {
        val flags: Short = 0x8180.toShort()
        val qSectionSize = (querySection.size - 12).coerceAtLeast(0)
        val buf =
            ByteBuffer.allocate(12 + qSectionSize + answerSection.size)
                .order(ByteOrder.BIG_ENDIAN)

        buf.putShort(txId)
        buf.putShort(flags)
        buf.putShort(1)
        buf.putShort(1)
        buf.putShort(0)
        buf.putShort(0)

        if (qSectionSize > 0) buf.put(querySection, 12, qSectionSize)
        buf.put(answerSection)

        return buf.array()
    }
}
