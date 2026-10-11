package love.yinlin.foundation.compression

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal fun compressionHex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

internal fun assertDecoded(codec: CompressionCodec, encoded: ByteArray, expected: ByteArray) {
    assertContentEquals(expected, codec.decompress(encoded))
    val destination = ByteArray(expected.size + 6) { 0x5A }
    assertEquals(expected.size, codec.decompressPlaceable(encoded, destination, 3))
    assertContentEquals(expected, destination.copyOfRange(3, 3 + expected.size))
    assertContentEquals(ByteArray(3) { 0x5A }, destination.copyOfRange(0, 3))
    assertContentEquals(ByteArray(3) { 0x5A }, destination.copyOfRange(3 + expected.size, destination.size))
}

internal fun compressionCodecs(limit: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT): List<CompressionCodec> = [
    RLE(limit),
    LZ4(limit),
    Snappy(limit),
    Deflate(limit),
    Zlib(limit),
    Gzip(limit)
]