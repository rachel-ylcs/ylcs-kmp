package love.yinlin.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestChecksums {
    @Test
    fun standardVectorsAndReset() {
        val input = "123456789".encodeToByteArray()
        val crc = CRC32().update(input)
        val adler = Adler32().update(input)
        assertEquals(0xcbf43926u.toInt(), crc.value)
        assertEquals(0x091e01de, adler.value)
        assertEquals(crc.value, crc.reset())
        assertEquals(adler.value, adler.reset())
        assertEquals(0, crc.value)
        assertEquals(1, adler.value)
    }

    @Test
    fun bulkAndPartitionedCrcMatchBitwiseReference() {
        for (size in [0, 1, 7, 8, 9, 63, 64, 65, 127, 128, 129, 4097]) {
            val data = ByteArray(size + 6) { (it * 73 + 19).toByte() }
            var expected = -1
            for (index in 3 ..< size + 3) {
                expected = expected xor (data[index].toInt() and 0xff)
                repeat(8) { expected = (expected ushr 1) xor (if ((expected and 1) == 0) 0 else 0xedb88320u.toInt()) }
            }
            expected = expected.inv()
            assertEquals(expected, CRC32.calculate(data, 3, size + 3), "bulk size=$size")
            val crc = CRC32()
            var start = 3
            while (start < size + 3) {
                val end = minOf(start + 71, size + 3)
                crc.update(data, start, end)
                start = end
            }
            assertEquals(expected, crc.value, "partitioned size=$size")
        }
    }

    @Test
    fun adlerBlockBoundariesMatchWideIntegerReference() {
        for (size in [5551, 5552, 5553, 11104, 65536]) {
            val data = ByteArray(size) { 0xff.toByte() }
            var a = 1L
            var b = 0L
            for (byte in data) {
                a = (a + (byte.toInt() and 0xff)) % 65521
                b = (b + a) % 65521
            }
            assertEquals(((b shl 16) or a).toInt(), Adler32.calculate(data), "size=$size")
        }
    }

    @Test
    fun overloadsAndInvalidRangesPreserveState() {
        val data = ByteArray(9) { (it * 31).toByte() }
        val crc = CRC32().update(data[0]).update(data[1], data[2]).update(data[3], data[4], data[5]).update(*data.copyOfRange(6, 9))
        val adler = Adler32().update(data[0]).update(data[1], data[2]).update(data[3], data[4], data[5]).update(*data.copyOfRange(6, 9))
        assertEquals(CRC32.calculate(data), crc.value)
        assertEquals(Adler32.calculate(data), adler.value)
        val crcBefore = crc.value
        val adlerBefore = adler.value
        for ([start, end] in [-1 to 3, 4 to 3, 0 to 10, Int.MAX_VALUE to Int.MAX_VALUE]) {
            assertFailsWith<IllegalArgumentException> { crc.update(data, start, end) }
            assertFailsWith<IllegalArgumentException> { adler.update(data, start, end) }
        }
        crc.update(data, data.size, data.size)
        adler.update(data, data.size, data.size)
        assertEquals(crcBefore, crc.value)
        assertEquals(adlerBefore, adler.value)
    }
}