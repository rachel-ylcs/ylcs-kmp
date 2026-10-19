package love.yinlin.io

import java.util.zip.Checksum
import kotlin.test.Test
import kotlin.test.assertEquals

class TestChecksumInterop {
    @Test
    fun crcMatchesJdkAtEverySliceAlignment() {
        for (offset in 0 ..< 8) {
            for (size in [0, 1, 7, 8, 63, 64, 65, 255, 4096, 1048576]) {
                val data = ByteArray(size + 16) { (it * 31 + 7).toByte() }
                val expected: Checksum = java.util.zip.CRC32()
                expected.update(data, offset, size)
                assertEquals(
                    expected.value.toInt(),
                    CRC32.calculate(data, offset, offset + size),
                    "offset=$offset size=$size"
                )
            }
        }
    }

    @Test
    fun adlerMatchesJdkWithMultipleMaximumSumBlocks() {
        for (size in [0, 1, 5551, 5552, 5553, 11104, 1048576]) {
            val data = ByteArray(size) { 0xff.toByte() }
            val expected = java.util.zip.Adler32().apply { update(data) }.value.toInt()
            assertEquals(expected, Adler32.calculate(data), "size=$size")
        }
    }
}