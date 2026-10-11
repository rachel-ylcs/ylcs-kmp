package love.yinlin.foundation.compression

import love.yinlin.encoding.convert
import kotlin.random.Random
import kotlin.test.*

class TestCompressionCodec {
    @Test
    fun roundTripCommonPayloads() {
        val sources = [
            [],
            [0],
            [-1],
            ByteArray(256) { it.toByte() },
            "压缩测试：Hello, compression!\u0000".repeat(100).convert(),
            ByteArray(70000) { 0xFF.toByte() },
            ByteArray(140000) { (it % 7).toByte() },
        ]
        for (codec in compressionCodecs()) for (source in sources) {
            val original = source.copyOf()
            val encoded = codec.compress(source)
            assertTrue(encoded.size <= codec.maxCompressedSize(source.size), codec::class.simpleName)
            assertContentEquals(source, codec.decompress(encoded), codec::class.simpleName)
            assertContentEquals(original, source)
        }
    }

    @Test
    fun roundTripLiteralAndBlockBoundaries() {
        val random = Random(1951)
        val sizes = [2, 3, 4, 12, 15, 60, 61, 127, 128, 129, 255, 256, 257, 65534, 65535, 65536, 131071]
        for (size in sizes) {
            val sources = [random.nextBytes(size), ByteArray(size) { (it % 3).toByte() }]
            for (codec in compressionCodecs()) for (source in sources) {
                val encoded = codec.compress(source)
                assertTrue(encoded.size <= codec.maxCompressedSize(size), "${codec::class.simpleName}: $size")
                assertContentEquals(source, codec.decompress(encoded), "${codec::class.simpleName}: $size")
            }
        }
    }

    @Test
    fun placeableReusesDestinationAndPreservesUnusedRegions() {
        val sources = [Random(32).nextBytes(4096), [-1], ByteArray(1000) { 3 }, []]
        val offset = 7
        val sentinel: Byte = 0x5A
        for (codec in compressionCodecs()) {
            val compressed = ByteArray(offset + codec.maxCompressedSize(4096) + 11)
            val decompressed = ByteArray(offset + 4096 + 11)
            for (source in sources) {
                compressed.fill(sentinel)
                decompressed.fill(sentinel)
                val original = source.copyOf()
                val count = codec.compressPlaceable(source, compressed, offset)
                val encoded = compressed.copyOfRange(offset, offset + count)
                assertContentEquals(codec.compress(source), encoded)
                assertTrue(compressed.take(offset).all { it == sentinel })
                assertTrue(compressed.drop(offset + count).all { it == sentinel })

                val decodedCount = codec.decompressPlaceable(encoded, decompressed, offset)
                assertEquals(source.size, decodedCount)
                assertContentEquals(source, decompressed.copyOfRange(offset, offset + decodedCount))
                assertTrue(decompressed.take(offset).all { it == sentinel })
                assertTrue(decompressed.drop(offset + decodedCount).all { it == sentinel })
                assertContentEquals(original, source)
                assertContentEquals(compressed.copyOfRange(offset, offset + count), encoded)
            }
        }
    }

    @Test
    fun placeableAcceptsExactCapacityAndEmptyOutputAtEnd() {
        for (codec in compressionCodecs()) for (source in [[], Random(64).nextBytes(64)]) {
            val encoded = codec.compress(source)
            val compressed = ByteArray(encoded.size + 3)
            assertEquals(encoded.size, codec.compressPlaceable(source, compressed, 3))
            assertContentEquals(encoded, compressed.copyOfRange(3, compressed.size))

            val decompressed = ByteArray(source.size + 3)
            assertEquals(source.size, codec.decompressPlaceable(encoded, decompressed, 3))
            assertContentEquals(source, decompressed.copyOfRange(3, decompressed.size))
        }
        assertEquals(0, RLE().compressPlaceable([], ByteArray(3), 3))
    }

    @Test
    fun insufficientCapacityThrowsItsOwnException() {
        val source = Random(128).nextBytes(128)
        for (codec in compressionCodecs()) {
            val encoded = codec.compress(source)
            val compressed = ByteArray(encoded.size + 2) { 0x5A }
            assertFailsWith<CompressionOutputTooSmallException> { codec.compressPlaceable(source, compressed, 3) }
            assertTrue(compressed.take(3).all { it == 0x5A.toByte() })

            val decompressed = ByteArray(source.size + 2) { 0x5A }
            assertFailsWith<CompressionOutputTooSmallException> { codec.decompressPlaceable(encoded, decompressed, 3) }
            assertTrue(decompressed.take(3).all { it == 0x5A.toByte() })
        }
    }

    @Test
    fun invalidOffsetsAndAliasedArraysAreRejected() {
        val source: ByteArray = [1, 2, 3]
        for (codec in compressionCodecs()) {
            val dst = ByteArray(32)
            for (offset in [-1, 33, Int.MAX_VALUE]) {
                assertFailsWith<CompressionInvalidArgumentException> { codec.compressPlaceable(source, dst, offset) }
                assertFailsWith<CompressionInvalidArgumentException> { codec.decompressPlaceable(source, dst, offset) }
            }
            assertFailsWith<CompressionInvalidArgumentException> { codec.compressPlaceable(source, source) }
            assertFailsWith<CompressionInvalidArgumentException> { codec.decompressPlaceable(source, source) }
            assertContentEquals([1, 2, 3], source)
        }
    }

    @Test
    fun invalidLimitsAndSizeBoundsAreRejectedWithoutAllocating() {
        val factories: List<(Int) -> CompressionCodec> = [::RLE, ::LZ4, ::Snappy, ::Deflate, ::Zlib, ::Gzip]
        for (factory in factories) assertFailsWith<CompressionInvalidArgumentException> { factory(-1) }
        for (codec in compressionCodecs()) {
            assertFailsWith<CompressionInvalidArgumentException> { codec.maxCompressedSize(-1) }
            assertFailsWith<CompressionInvalidArgumentException> { codec.maxCompressedSize(Int.MAX_VALUE) }
            assertTrue(codec.maxCompressedSize(0) >= 0)
        }
    }

    @Test
    fun decompressionLimitsApplyToBothApisAndResetForEachCall() {
        for (codec in compressionCodecs(32)) {
            val atLimit = ByteArray(32) { 1 }
            val overLimit = ByteArray(33) { 1 }
            val encoded = codec.compress(overLimit)
            assertFailsWith<CompressionOutputLimitExceededException> { codec.decompress(encoded) }
            assertFailsWith<CompressionOutputLimitExceededException> { codec.decompressPlaceable(encoded, ByteArray(64)) }
            assertContentEquals(atLimit, codec.decompress(codec.compress(atLimit)))
            assertEquals(32, codec.decompressPlaceable(codec.compress(atLimit), ByteArray(32)))
        }
        for (codec in compressionCodecs(0)) {
            assertContentEquals([], codec.decompress(codec.compress([])))
            assertEquals(0, codec.decompressPlaceable(codec.compress([]), ByteArray(0)))
            assertFailsWith<CompressionOutputLimitExceededException> { codec.decompress(codec.compress([1])) }
        }
    }
}
