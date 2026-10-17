package love.yinlin.compose.graphics.qrcode

import kotlin.test.*

class QrcodeEncoderTest {
    @Test
    fun preservesExistingMatricesForEveryTextModeAndCorrectionLevel() {
        val texts: List<String> = [
            "", "01234567", "HELLO WORLD", "hello world", "中文二维码 🎵",
            "rachel://yinlin.love/openProfile?uid=42",
        ]
        val sizes: Array<IntArray> = [
            [21, 21, 21, 21, 25, 29],
            [21, 21, 21, 21, 25, 29],
            [21, 21, 21, 21, 25, 33],
            [21, 21, 25, 25, 29, 37],
        ]
        val hashes: Array<IntArray> = [
            [-1928560598, -1379918510, 1409225792, 2116620548, -1867251892, 98244490],
            [86273520, -1302592048, 1254963488, -171853694, 1954803536, 1381317738],
            [-701327638, -1117064910, -1969350548, 1362828800, -275541120, -1143116336],
            [-737630784, -1035938170, 886535512, 1350857580, 1030602290, 1485146304],
        ]
        for (level in QrcodeECLevel.entries) {
            for ([index, text] in texts.withIndex()) {
                val matrix = QrcodeEncoder.encode(text, level)
                assertEquals(sizes[level.ordinal][index], matrix.size, "$level: $text")
                val bits = buildString {
                    for (y in 0 ..< matrix.size) {
                        for (x in 0 ..< matrix.size) append(if (matrix[x, y]) '1' else '0')
                    }
                }
                assertEquals(hashes[level.ordinal][index], bits.hashCode(), "$level: $text")
            }
        }
    }

    @Test
    fun selectsSmallestVersionAtByteCapacityBoundaries() {
        val version1: IntArray = [17, 14, 11, 7]
        val version9: IntArray = [230, 180, 130, 98]
        for (level in QrcodeECLevel.entries) {
            val first = version1[level.ordinal]
            assertEquals(21, QrcodeEncoder.encode("a".repeat(first), level).size)
            assertEquals(25, QrcodeEncoder.encode("a".repeat(first + 1), level).size)

            val ninth = version9[level.ordinal]
            assertEquals(53, QrcodeEncoder.encode("a".repeat(ninth), level).size)
            assertEquals(57, QrcodeEncoder.encode("a".repeat(ninth + 1), level).size)
        }
    }

    @Test
    fun supportsVersion40AndRejectsOverflowInEveryMode() {
        val capacities: Array<IntArray> = [
            [7089, 4296, 2953],
            [5596, 3391, 2331],
            [3993, 2420, 1663],
            [3057, 1852, 1273],
        ]
        val characters: List<String> = ["7", "A", "a"]
        for (level in QrcodeECLevel.entries) {
            for ([mode, char] in characters.withIndex()) {
                val count = capacities[level.ordinal][mode]
                assertEquals(177, QrcodeEncoder.encode(char.repeat(count), level).size, "$level: $char")
                assertFailsWith<IllegalArgumentException>("$level: $char") {
                    QrcodeEncoder.encode(char.repeat(count + 1), level)
                }
            }
        }
    }

    @Test
    fun countsUtf8BytesInsteadOfUtf16Characters() {
        val level = QrcodeECLevel.H
        assertEquals(21, QrcodeEncoder.encode("码码", level).size)
        assertEquals(25, QrcodeEncoder.encode("码码码", level).size)
        assertEquals(21, QrcodeEncoder.encode("🎵", level).size)
        assertEquals(25, QrcodeEncoder.encode("🎵🎵", level).size)
        assertFailsWith<IllegalArgumentException> { QrcodeEncoder.encode("码".repeat(425), level) }
    }

    @Test
    fun retainsCompactNumericAndAlphanumericModes() {
        val level = QrcodeECLevel.L
        assertEquals(21, QrcodeEncoder.encode("1".repeat(41), level).size)
        assertEquals(25, QrcodeEncoder.encode("1".repeat(42), level).size)
        assertEquals(21, QrcodeEncoder.encode("A".repeat(25), level).size)
        assertEquals(25, QrcodeEncoder.encode("A".repeat(26), level).size)
    }
}
