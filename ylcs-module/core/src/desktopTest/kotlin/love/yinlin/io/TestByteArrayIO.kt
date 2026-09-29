package love.yinlin.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TestByteArrayIO {
    @Test
    fun constructorsAndSharedBytes() {
        val raw = ByteArray(4)
        val wrapped = ByteArrayIO(raw)

        assertSame(raw, wrapped.bytes)
        assertEquals(4, wrapped.size)

        wrapped.writeInt(0, 0x01020304)
        assertContentEquals([1, 2, 3, 4], raw)

        val created = ByteArrayIO(size = 2)
        assertEquals(2, created.size)
        assertContentEquals([0, 0], created.bytes)
    }

    @Test
    fun booleanByteAndChar() {
        val io = ByteArrayIO(5)
        io.writeBoolean(0, true)
        io.writeUByte(1, UByte.MAX_VALUE)
        io.writeChar(2, '中')
        io.writeBoolean(4, false)

        assertEquals(true, io.readBoolean(0))
        assertEquals((-1).toByte(), io.readByte(1))
        assertEquals(UByte.MAX_VALUE, io.readUByte(1))
        assertEquals('中', io.readChar(2))
        assertEquals(false, io.readBoolean(4))
    }

    @Test
    fun signedIntegersRespectEndian() {
        val io = ByteArrayIO(28)
        io.writeShort(0, 0x1234.toShort())
        io.writeShort(2, 0x1234.toShort(), Endian.LITTLE)
        io.writeInt(4, 0x12345678)
        io.writeInt(8, 0x12345678, Endian.LITTLE)
        io.writeLong(12, 0x0102030405060708L)
        io.writeLong(20, 0x0102030405060708L, Endian.LITTLE)

        assertContentEquals([
            0x12, 0x34, 0x34, 0x12,
            0x12, 0x34, 0x56, 0x78,
            0x78, 0x56, 0x34, 0x12,
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01,
        ], io.bytes)

        assertEquals(0x1234.toShort(), io.readShort(0))
        assertEquals(0x1234.toShort(), io.readShort(2, Endian.LITTLE))
        assertEquals(0x12345678, io.readInt(4))
        assertEquals(0x12345678, io.readInt(8, Endian.LITTLE))
        assertEquals(0x0102030405060708L, io.readLong(12))
        assertEquals(0x0102030405060708L, io.readLong(20, Endian.LITTLE))
    }

    @Test
    fun unsignedIntegersKeepAllBits() {
        val io = ByteArrayIO(15)
        io.writeUByte(0, UByte.MAX_VALUE)
        io.writeUShort(1, UShort.MAX_VALUE)
        io.writeUInt(3, UInt.MAX_VALUE)
        io.writeULong(7, ULong.MAX_VALUE)

        assertEquals(UByte.MAX_VALUE, io.readUByte(0))
        assertEquals(UShort.MAX_VALUE, io.readUShort(1))
        assertEquals(UInt.MAX_VALUE, io.readUInt(3))
        assertEquals(ULong.MAX_VALUE, io.readULong(7))
    }

    @Test
    fun floatingPointKeepsRawBits() {
        val io = ByteArrayIO(12)
        io.writeFloat(0, -0.0f)
        io.writeDouble(4, -12.5)

        assertEquals((-0.0f).toRawBits(), io.readFloat(0).toRawBits())
        assertEquals((-12.5).toRawBits(), io.readDouble(4).toRawBits())
    }

    @Test
    fun stringAndByteArray() {
        val text = "A中🙂"
        val textLength = text.encodeToByteArray().size
        val io = ByteArrayIO(textLength + 3)

        io.writeString(0, text)
        io.writeByteArray(textLength, [1, 2, 3])

        assertEquals(text, io.readString(0, textLength))
        assertContentEquals([1, 2, 3], io.readByteArray(textLength, 3))

        val copy = io.readByteArray(textLength, 3)
        copy[0] = 9
        assertEquals(1.toByte(), io.bytes[textLength])
    }

    @Test
    fun invalidRangesAndDataThrow() {
        val io = ByteArrayIO(4)

        assertFailsWith<IndexOutOfBoundsException> { io.readInt(1) }
        assertFailsWith<IndexOutOfBoundsException> { io.writeLong(0, 1L) }
        assertFailsWith<IndexOutOfBoundsException> { io.writeByteArray(3, [1, 2]) }
        assertFailsWith<IllegalArgumentException> { io.readString(0, -1) }
        assertContentEquals(ByteArray(4), io.bytes)
    }
}