package love.yinlin.foundation.compression

import love.yinlin.encoding.convert
import kotlin.random.Random
import kotlin.test.*

class TestCompressionErrors {
    @Test
    fun truncatedInputUsesItsOwnExceptionInBothApis() {
        val cases = [
            RLE() to "00", LZ4() to "10", Snappy() to "80",
            Deflate() to "01", Zlib() to "78", Gzip() to "1f",
        ]
        for ([codec, hex] in cases) {
            val data = compressionHex(hex)
            assertFailsWith<CompressionTruncatedInputException> { codec.decompress(data) }
            assertFailsWith<CompressionTruncatedInputException> { codec.decompressPlaceable(data, ByteArray(64)) }
        }
        val error = assertFailsWith<CompressionTruncatedInputException> { RLE().decompress(compressionHex("004201")) }
        assertEquals(3, error.offset)
        assertTrue(error.message.orEmpty().contains("byte 3"))
    }

    @Test
    fun malformedStreamsUseInvalidDataException() {
        val cases = [
            LZ4() to "1061000000", Snappy() to "0500610100", Snappy() to "01046162",
            Deflate() to "07", Deflate() to "0103000000616263", Deflate() to "030200",
            Zlib() to "7802", Gzip() to "0000",
        ]
        for ([codec, hex] in cases) {
            val data = compressionHex(hex)
            assertFailsWith<CompressionInvalidDataException> { codec.decompress(data) }
            assertFailsWith<CompressionInvalidDataException> { codec.decompressPlaceable(data, ByteArray(64)) }
        }
        val gzip = Gzip().compress([]).apply { this[3] = 0xE0.toByte() }
        assertFailsWith<CompressionInvalidDataException> { Gzip().decompress(gzip) }
    }

    @Test
    fun unsupportedMethodsAndPresetDictionariesUseTheirOwnException() {
        val cases = [
            Zlib() to compressionHex("7900"),
            Zlib() to compressionHex("7820"),
            Gzip() to Gzip().compress([]).apply { this[2] = 0 }
        ]
        for ([codec, data] in cases) {
            assertFailsWith<CompressionUnsupportedFormatException> { codec.decompress(data) }
            assertFailsWith<CompressionUnsupportedFormatException> { codec.decompressPlaceable(data, ByteArray(64)) }
        }
    }

    @Test
    fun damagedChecksumsAndGzipSizeAreDistinguished() {
        val source = "checksum".convert()
        val zlib = Zlib().compress(source).apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertEquals(zlib.size - 4, assertFailsWith<CompressionChecksumMismatchException> { Zlib().decompress(zlib) }.offset)
        assertFailsWith<CompressionChecksumMismatchException> { Zlib().decompressPlaceable(zlib, ByteArray(source.size), 0) }

        val gzip = Gzip().compress(source)
        val badCrc = gzip.copyOf().apply { this[size - 8] = (this[size - 8].toInt() xor 1).toByte() }
        assertEquals(gzip.size - 8, assertFailsWith<CompressionChecksumMismatchException> { Gzip().decompress(badCrc) }.offset)
        assertFailsWith<CompressionChecksumMismatchException> { Gzip().decompressPlaceable(badCrc, ByteArray(source.size)) }
        val badSize = gzip.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertFailsWith<CompressionInvalidDataException> { Gzip().decompress(badSize) }
        assertFailsWith<CompressionInvalidDataException> { Gzip().decompressPlaceable(badSize, ByteArray(source.size)) }
    }

    @Test
    fun snappyValidatesDeclaredSizesBeforeWritingOrAllocatingOutput() {
        val hugeLength = compressionHex("ffffffff07")
        assertFailsWith<CompressionOutputLimitExceededException> { Snappy().decompress(hugeLength) }
        assertFailsWith<CompressionOutputLimitExceededException> { Snappy().decompressPlaceable(hugeLength, ByteArray(64)) }
        assertFailsWith<CompressionInvalidDataException> { Snappy().decompress(compressionHex("ffffffff08")) }
        assertFailsWith<CompressionInvalidDataException> { Snappy().decompress(compressionHex("01")) }
        val dst = ByteArray(3) { 0x5A }
        assertFailsWith<CompressionOutputTooSmallException> { Snappy().decompressPlaceable(compressionHex("040c01020304"), dst) }
        assertContentEquals(ByteArray(3) { 0x5A }, dst)
    }

    @Test
    fun rawDeflateAndZlibRejectTrailingData() {
        for (codec in [Deflate(), Zlib()]) {
            val encoded = codec.compress([]) + 0
            assertFailsWith<CompressionInvalidDataException> { codec.decompress(encoded) }
            assertFailsWith<CompressionInvalidDataException> { codec.decompressPlaceable(encoded, ByteArray(64)) }
        }
    }

    @Test
    fun malformedInputsNeverEscapeAsArrayOrArithmeticExceptions() {
        val random = Random(1952)
        val inputs = List(200) { random.nextBytes(random.nextInt(33)) }
        for (codec in compressionCodecs(512)) for (data in inputs) {
            try {
                assertTrue(codec.decompress(data).size <= 512)
            } catch (_: CompressionException) { }
            try {
                assertTrue(codec.decompressPlaceable(data, ByteArray(520), 4) in 0..512)
            } catch (_: CompressionException) { }
        }
    }
}
