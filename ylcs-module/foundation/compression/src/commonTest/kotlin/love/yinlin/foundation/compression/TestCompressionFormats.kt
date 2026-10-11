package love.yinlin.foundation.compression

import love.yinlin.encoding.convert
import love.yinlin.io.CRC32
import kotlin.test.*

class TestCompressionFormats {
    @Test
    fun emptyEncodingsAreDeterministicAndConformToTheirFormats() {
        val cases = [
            RLE() to "", LZ4() to "00", Snappy() to "00", Deflate() to "0300",
            Zlib() to "7801030000000001", Gzip() to "1f8b08000000000000ff03000000000000000000",
        ]
        for ([codec, hex] in cases) {
            val encoded = compressionHex(hex)
            assertContentEquals(encoded, codec.compress([]), codec::class.simpleName)
            assertDecoded(codec, encoded, [])
        }
    }

    @Test
    fun packBitsSupportsLiteralsRunsAndNoOpControls() {
        val encoded = compressionHex("020080ff80fe7f")
        assertDecoded(RLE(), encoded, [0, 0x80.toByte(), -1, 127, 127, 127])
        assertDecoded(RLE(), compressionHex("8142"), ByteArray(128) { 0x42 })
        assertDecoded(RLE(), compressionHex("808080"), [])
    }

    @Test
    fun lz4SupportsLiteralExtensionsAndOverlappingMatches() {
        assertDecoded(LZ4(), byteArrayOf(0x50) + "hello".convert(), "hello".convert())
        val literal = ByteArray(15) { it.toByte() }
        assertDecoded(LZ4(), byteArrayOf(0xF0.toByte(), 0) + literal, literal)
        val tail = "bcdef".convert()
        assertDecoded(LZ4(), compressionHex("1f6101000050") + tail, ByteArray(20) { 0x61 } + tail)
        val alternating = ByteArray(21) { if (it % 2 == 0) 0x61 else 0x62 }
        assertDecoded(LZ4(), compressionHex("2f616202000050") + tail, alternating + tail)
    }

    @Test
    fun snappySupportsAllCopyTagsAndExtendedLiterals() {
        assertDecoded(Snappy(), compressionHex("14006105011a01001b01000000"), ByteArray(20) { 0x61 })
        val literal61 = ByteArray(61) { it.toByte() }
        assertDecoded(Snappy(), compressionHex("3df03c") + literal61, literal61)
        val literal257 = ByteArray(257) { it.toByte() }
        assertDecoded(Snappy(), compressionHex("8102f40001") + literal257, literal257)
        val literal65537 = ByteArray(65537) { it.toByte() }
        assertDecoded(Snappy(), compressionHex("818004f8000001") + literal65537, literal65537)
    }

    @Test
    fun deflateDecodesIndependentStoredFixedAndDynamicFixtures() {
        // Python zlib 1.3.1 生成的独立样本，分别使用 level=0、Z_FIXED 和默认策略。
        assertDecoded(Deflate(), compressionHex("010300fcff616263"), "abc".convert())
        val expected = "The quick brown fox jumps over the lazy dog. ".repeat(200).convert()
        val fixed = compressionHex(
            "0bc94855282ccd4cce56482aca2fcf5348cbaf50c82acd2d2856c82f4b2d5228014ae72456552aa4e4a7eb29848c2a1e553caa7854f1a8e251c5a38a47158f2a1e553caa7854f1a8e251c5a38a47158f2a1e553caa7854f1a8e251c5a38a47158f2a1e553caa7854f1a8e251c5a38a47158f2a1e553c34150300",
        )
        val dynamic = compressionHex(
            "edcacb1182301400c0565e0554930640837f03d1a850bdb4c1cc9e77d339c7dc2e875b0cb57c9f31965f5cdb637a45f9e41aef8deffdbac4b19cba48b22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb2bccffc07",
        )
        assertEquals(1, (fixed[0].toInt() ushr 1) and 3)
        assertEquals(2, (dynamic[0].toInt() ushr 1) and 3)
        assertDecoded(Deflate(), fixed, expected)
        assertDecoded(Deflate(), dynamic, expected)
    }

    @Test
    fun zlibAndGzipDecodeIndependentDynamicFixtures() {
        val expected = "The quick brown fox jumps over the lazy dog. ".repeat(200).convert()
        assertDecoded(Zlib(), compressionHex(
            "78daedcacb1182301400c0565e0554930640837f03d1a850bdb4c1cc9e77d339c7dc2e875b0cb57c9f31965f5cdb637a45f9e41aef8deffdbac4b19cba48b22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb2bccffc0786419f2d",
        ), expected)
        assertDecoded(Gzip(), compressionHex(
            "1f8b080000000000020aedcacb1182301400c0565e0554930640837f03d1a850bdb4c1cc9e77d339c7dc2e875b0cb57c9f31965f5cdb637a45f9e41aef8deffdbac4b19cba48b22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb22ccbb2bccffc07692f8b9328230000",
        ), expected)
    }

    @Test
    fun gzipSupportsOptionalFieldsHeaderChecksumAndConcatenatedMembers() {
        val source = "optional gzip header".convert()
        val standard = Gzip().compress(source)
        val header = standard.copyOfRange(0, 10).apply { this[3] = 0x1E }
        val extra: ByteArray = [3, 0, 10, 20, 30]
        val strings = "sample.bin\u0000comment\u0000".convert()
        val prefix = header + extra + strings
        val crc = CRC32.calculate(prefix)
        val encoded = prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte()) + standard.copyOfRange(10, standard.size)
        assertDecoded(Gzip(), encoded, source)
        val members = encoded + Gzip().compress([]) + Gzip().compress(source)
        assertDecoded(Gzip(), members, source + source)
        val badHeader = encoded.copyOf().apply { this[prefix.size] = (this[prefix.size].toInt() xor 1).toByte() }
        val error = assertFailsWith<CompressionChecksumMismatchException> { Gzip().decompress(badHeader) }
        assertEquals(prefix.size, error.offset)
        assertFailsWith<CompressionChecksumMismatchException> { Gzip().decompressPlaceable(badHeader, ByteArray(source.size)) }
    }

    @Test
    fun gzipLimitsCoverAllMembersAndHistoryCannotCrossMembers() {
        val source = ByteArray(20) { 1 }
        val members = Gzip().compress(source) + Gzip().compress(source)
        assertFailsWith<CompressionOutputLimitExceededException> { Gzip(32).decompress(members) }
        assertFailsWith<CompressionOutputLimitExceededException> { Gzip(32).decompressPlaceable(members, ByteArray(64)) }
        val first = Gzip().compress([0x61])
        // 030200 是没有前置字面量却引用距离 1 的 fixed Deflate 块。
        val invalidSecond = first.copyOfRange(0, 10) + compressionHex("030200") + ByteArray(8)
        assertFailsWith<CompressionInvalidDataException> { Gzip().decompress(first + invalidSecond) }
        assertFailsWith<CompressionInvalidDataException> { Gzip().decompressPlaceable(first + invalidSecond, ByteArray(16), 3) }
    }
}
