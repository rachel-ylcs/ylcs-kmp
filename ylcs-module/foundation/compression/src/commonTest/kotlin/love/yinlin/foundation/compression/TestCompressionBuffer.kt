package love.yinlin.foundation.compression

import kotlin.test.*

class TestCompressionBuffer {
    @Test
    fun integerReadsAndWritesPreserveEndianAndUnsignedBits() {
        val destination = ByteArray(17) { 0x5A }
        val output = CompressionOutput(destination, 3, 14)
        output.writeByte(0xFF)
        output.writeShortLE(0xFEDC)
        output.writeIntLE(0x89ABCDEF.toInt())
        output.writeIntBE(0x89ABCDEF.toInt())
        assertSame(destination, output.bytes)
        assertEquals(11, output.size)
        assertContentEquals(
            [-1, 0xDC.toByte(), 0xFE.toByte(), 0xEF.toByte(), 0xCD.toByte(), 0xAB.toByte(), 0x89.toByte(),
                0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()],
            output.toByteArray(),
        )
        val input = CompressionInput(destination, 3, 14)
        assertEquals(255, input.readByte())
        assertEquals(0xFEDC, input.readShortLE())
        assertEquals(0x89ABCDEF.toInt(), input.readIntLE())
        assertEquals(0x89ABCDEF.toInt(), input.readIntBE())
        assertEquals(0, input.remaining)
        assertTrue(destination.take(3).all { it == 0x5A.toByte() })
        assertTrue(destination.drop(14).all { it == 0x5A.toByte() })
    }

    @Test
    fun integerReadsRespectLogicalEndAndDoNotPartiallyConsume() {
        val input = CompressionInput(ByteArray(8), 2, 3)
        assertEquals(2, assertFailsWith<CompressionTruncatedInputException> { input.readShortLE() }.offset)
        assertEquals(2, input.position)
        assertEquals(2, assertFailsWith<CompressionTruncatedInputException> { input.readIntLE() }.offset)
        assertEquals(2, assertFailsWith<CompressionTruncatedInputException> { input.readIntBE() }.offset)
        assertEquals(0, input.readByte())
        assertEquals(3, assertFailsWith<CompressionTruncatedInputException> { input.readByte() }.offset)
    }

    @Test
    fun growableOutputWritesToTheNewBackingArrayAfterGrowth() {
        val output = CompressionOutput(2048)
        output.fill(0xAB, 255)
        val initial = output.bytes
        output.writeIntLE(0x12345678)
        output.writeIntBE(0x12345678)
        output.writeShortLE(0xFEDC)
        output.fill(0xCD, 900)
        assertNotSame(initial, output.bytes)
        val input = CompressionInput(output.toByteArray(), 255)
        assertEquals(0x12345678, input.readIntLE())
        assertEquals(0x12345678, input.readIntBE())
        assertEquals(0xFEDC, input.readShortLE())
        assertEquals(900, input.remaining)
        assertTrue(output.toByteArray().take(255).all { it == 0xAB.toByte() })
        assertTrue(output.toByteArray().drop(265).all { it == 0xCD.toByte() })
    }

    @Test
    fun overlappingMatchesRepeatOnlyProducedHistory() {
        for (distance in [1, 2, 3, 7]) {
            val destination = ByteArray(400) { 0x5A }
            val output = CompressionOutput(destination, 5, 395)
            val prefix = ByteArray(distance) { (it + 1).toByte() }
            output.write(prefix, 0, prefix.size)
            output.copyMatch(distance, 257)
            assertSame(destination, output.bytes)
            assertContentEquals(ByteArray(distance + 257) { prefix[it % distance] }, output.toByteArray())
            assertTrue(destination.take(5).all { it == 0x5A.toByte() })
            assertTrue(destination.drop(5 + output.size).all { it == 0x5A.toByte() })
        }
    }

    @Test
    fun matchDistancesCannotUseUnwrittenDataOrPreviousMembers() {
        val output = CompressionOutput(ByteArray(32), 4, 28)
        assertFailsWith<CompressionInvalidDataException> { output.copyMatch(1, 1) }
        output.fill(1, 4)
        output.writeByte(2)
        for (distance in [0, -1, 6]) assertFailsWith<CompressionInvalidDataException> { output.copyMatch(distance, 1) }
        assertFailsWith<CompressionInvalidDataException> { output.copyMatch(2, 1, historyStart = 4) }
        output.copyMatch(1, 3, historyStart = 4)
        assertContentEquals([1, 1, 1, 1, 2, 2, 2, 2], output.toByteArray())
    }

    @Test
    fun varIntBoundariesAndInvalidLengths() {
        for (value in [0, 127, 128, 16383, 16384, 2097151, 2097152, Int.MAX_VALUE]) {
            val output = CompressionOutput(5)
            output.writeVarInt(value)
            val input = CompressionInput(output.toByteArray())
            assertEquals(value, input.readVarInt())
            assertEquals(0, input.remaining)
        }
        assertFailsWith<CompressionTruncatedInputException> { CompressionInput([0x80.toByte()]).readVarInt() }
        val overflow: ByteArray = [-1, -1, -1, -1, 8]
        assertEquals(4, assertFailsWith<CompressionInvalidDataException> { CompressionInput(overflow).readVarInt() }.offset)
        assertFailsWith<CompressionInvalidDataException> { CompressionInput(ByteArray(6) { 0x80.toByte() }).readVarInt() }
    }

    @Test
    fun capacityAndConfiguredLimitRemainDistinct() {
        val fixed = CompressionOutput(ByteArray(8), 2, 10)
        assertFailsWith<CompressionOutputTooSmallException> { fixed.checkSize(7) }
        fixed.fill(0, 6)
        assertFailsWith<CompressionOutputTooSmallException> { fixed.writeByte(0) }
        val limited = CompressionOutput(ByteArray(16), 2, 3)
        assertFailsWith<CompressionOutputLimitExceededException> { limited.checkSize(4) }
        limited.fill(0, 3)
        assertFailsWith<CompressionOutputLimitExceededException> { limited.writeByte(0) }
        assertEquals(Int.MAX_VALUE, CompressionCodec.compressedBound(Int.MAX_VALUE - 5, 5))
        assertFailsWith<CompressionInvalidArgumentException> { CompressionCodec.compressedBound(Int.MAX_VALUE - 4, 5) }
    }
}
