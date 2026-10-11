package love.yinlin.foundation.compression

/**
 * RFC 1951 原始 Deflate。编码使用哈希 LZ77、固定 Huffman 和不可压缩块回退；
 * 解码支持 stored、fixed、dynamic 全部三种块格式。
 */
class Deflate(private val maxDecompressedSize: Int = CompressionCodec.DEFAULT_DECOMPRESSION_LIMIT) : CompressionCodec {
    private companion object {
        val LENGTH_BASE: IntArray = [3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258]
        val LENGTH_EXTRA: IntArray = [0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0]
        val DISTANCE_BASE: IntArray = [1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577]
        val DISTANCE_EXTRA: IntArray = [0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13]
        val CODE_LENGTH_ORDER: IntArray = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15]
        val LENGTH_CODES = IntArray(259).apply {
            var code = 0
            for (length in 3..258) {
                while (code < 28 && length >= LENGTH_BASE[code + 1]) code++
                this[length] = code
            }
        }
        val FIXED_LENGTHS = IntArray(288) { symbol ->
            when (symbol) {
                in 0..143 -> 8
                in 144..255 -> 9
                in 256..279 -> 7
                else -> 8
            }
        }
        val FIXED_CODES = IntArray(288) { symbol ->
            val code = when (symbol) {
                in 0..143 -> 0x30 + symbol
                in 144..255 -> 0x190 + symbol - 144
                in 256..279 -> symbol - 256
                else -> 0xC0 + symbol - 280
            }
            reverseBits(code, FIXED_LENGTHS[symbol])
        }
        val FIXED_LITERALS = DeflateHuffman(FIXED_LENGTHS)
        val FIXED_DISTANCES = DeflateHuffman(IntArray(32) { 5 })

        fun reverseBits(value: Int, width: Int): Int {
            var source = value
            var result = 0
            repeat(width) {
                result = (result shl 1) or (source and 1)
                source = source ushr 1
            }
            return result
        }
    }

    internal class DeflateBitReader(private val input: CompressionInput) {
        private var buffer = 0
        private var bitCount = 0
        val byteOffset: Int get() = input.position - bitCount / 8

        private fun fill(width: Int) {
            while (bitCount < width && input.remaining > 0) {
                buffer = buffer or (input.readByte() shl bitCount)
                bitCount += 8
            }
        }

        private fun drop(width: Int) {
            buffer = buffer ushr width
            bitCount -= width
        }

        fun read(width: Int): Int {
            fill(width)
            if (bitCount < width) throw CompressionTruncatedInputException("Truncated Deflate bit stream", byteOffset)
            val value = buffer and ((1 shl width) - 1)
            drop(width)
            return value
        }

        fun symbol(tree: DeflateHuffman): Int {
            fill(tree.maxBits)
            val entry = tree.table[buffer and (tree.table.size - 1)]
            val width = entry ushr 16
            if (width == 0) throw CompressionInvalidDataException("Invalid Huffman code", offset = byteOffset)
            if (bitCount < width) throw CompressionTruncatedInputException("Truncated Huffman code", byteOffset)
            drop(width)
            return entry and 0xFFFF
        }

        fun align() {
            drop(bitCount and 7)
        }

        fun copyTo(output: CompressionOutput, count: Int) {
            if (count > input.remaining + bitCount / 8) throw CompressionTruncatedInputException("Truncated stored block", byteOffset)
            var remaining = count
            while (remaining > 0 && bitCount >= 8) {
                output.writeByte(read(8))
                remaining--
            }
            input.copyTo(output, remaining)
        }
    }

    internal class DeflateBitWriter(val output: CompressionOutput) {
        private var buffer = 0
        var bitCount = 0
            private set

        fun write(value: Int, width: Int) {
            buffer = buffer or ((value and ((1 shl width) - 1)) shl bitCount)
            bitCount += width
            while (bitCount >= 8) {
                output.writeByte(buffer)
                buffer = buffer ushr 8
                bitCount -= 8
            }
        }

        fun align() {
            if (bitCount != 0) {
                output.writeByte(buffer)
                buffer = 0
                bitCount = 0
            }
        }
    }

    /** canonical Huffman 查找表。一个读取操作完成符号解码，最长码长 15。 */
    internal class DeflateHuffman(lengths: IntArray, allowSingle: Boolean = true, allowEmpty: Boolean = false) {
        val maxBits: Int
        val table: IntArray

        init {
            val counts = IntArray(16)
            var maximum = 0
            var symbols = 0
            for (length in lengths) {
                if (length !in 0 .. 15) throw CompressionInvalidDataException("Invalid Huffman code length")
                if (length != 0) {
                    counts[length]++
                    maximum = maxOf(maximum, length)
                    symbols++
                }
            }
            if (symbols == 0 && !allowEmpty) throw CompressionInvalidDataException("Empty Huffman tree")
            var unused = 1
            for (width in 1 .. 15) {
                unused = (unused shl 1) - counts[width]
                if (unused < 0) throw CompressionInvalidDataException("Oversubscribed Huffman tree")
            }
            if (unused != 0 && symbols != 0 && !(allowSingle && symbols == 1 && maximum == 1)) {
                throw CompressionInvalidDataException("Incomplete Huffman tree")
            }
            maxBits = maximum
            table = IntArray(1 shl maximum)
            val nextCode = IntArray(16)
            var code = 0
            for (width in 1 .. maximum) {
                code = (code + counts[width - 1]) shl 1
                nextCode[width] = code
            }
            for (symbol in lengths.indices) {
                val width = lengths[symbol]
                if (width == 0) continue
                val reversed = reverseBits(nextCode[width]++, width)
                val entry = (width shl 16) or symbol
                var index = reversed
                while (index < table.size) {
                    table[index] = entry
                    index += 1 shl width
                }
            }
        }
    }

    init {
        CompressionCodec.checkDecompressionLimit(maxDecompressedSize)
    }

    override fun maxCompressedSize(sourceSize: Int): Int = CompressionCodec.compressedBound(sourceSize, maxOf(1L, (sourceSize.toLong() + 65534) / 65535) * 5)

    private fun hash(data: ByteArray, offset: Int): Int {
        val value = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8) or ((data[offset + 2].toInt() and 0xFF) shl 16)
        return (value * -1640531535) ushr 17
    }

    private fun distanceCode(distance: Int): Int {
        if (distance <= 4) return distance - 1
        val value = distance - 1
        val log = 31 - value.countLeadingZeroBits()
        return (log shl 1) + ((value ushr (log - 1)) and 1)
    }

    private fun writeSymbol(bits: DeflateBitWriter, symbol: Int) {
        bits.write(FIXED_CODES[symbol], FIXED_LENGTHS[symbol])
    }

    internal fun encode(data: ByteArray, output: CompressionOutput) {
        val bits = DeflateBitWriter(output)
        val table = if (data.size >= 3) IntArray(32768) { -1 } else IntArray(0)
        val tokens = IntArray(minOf(data.size, 65535))
        var position = 0
        do {
            val begin = position
            val end = minOf(data.size.toLong(), begin.toLong() + 65535).toInt()
            var tokenCount = 0
            var fixedCost = 3 + FIXED_LENGTHS[256]
            while (position < end) {
                var length = 0
                var distance = 0
                if (position <= end - 3) {
                    val key = hash(data, position)
                    val candidate = table[key]
                    table[key] = position
                    if (candidate >= 0 && position - candidate <= 32768 && data[candidate] == data[position] && data[candidate + 1] == data[position + 1] && data[candidate + 2] == data[position + 2]) {
                        length = 3
                        val limit = minOf(258, end - position)
                        while (length < limit && data[candidate + length] == data[position + length]) length++
                        distance = position - candidate
                    }
                }
                if (length >= 3) {
                    val lengthCode = LENGTH_CODES[length]
                    val distanceCode = distanceCode(distance)
                    tokens[tokenCount++] = (length shl 16) or distance
                    fixedCost += FIXED_LENGTHS[257 + lengthCode] + LENGTH_EXTRA[lengthCode] + 5 + DISTANCE_EXTRA[distanceCode]
                    val next = position + length
                    position++
                    while (position < next) {
                        if (position <= data.size - 3) table[hash(data, position)] = position
                        position++
                    }
                }
                else {
                    val literal = data[position++].toInt() and 0xFF
                    tokens[tokenCount++] = literal
                    fixedCost += FIXED_LENGTHS[literal]
                }
            }

            val final = if (position == data.size) 1 else 0
            val storedCost = 3 + ((8 - ((bits.bitCount + 3) and 7)) and 7) + 32 + (end - begin) * 8
            if (fixedCost <= storedCost) {
                bits.write(final or 2, 3)
                var index = 0
                while (index < tokenCount) {
                    val token = tokens[index++]
                    if (token < 256) writeSymbol(bits, token)
                    else {
                        val length = token ushr 16
                        val distance = token and 0xFFFF
                        val lengthCode = LENGTH_CODES[length]
                        writeSymbol(bits, 257 + lengthCode)
                        bits.write(length - LENGTH_BASE[lengthCode], LENGTH_EXTRA[lengthCode])
                        val distanceCode = distanceCode(distance)
                        bits.write(reverseBits(distanceCode, 5), 5)
                        bits.write(distance - DISTANCE_BASE[distanceCode], DISTANCE_EXTRA[distanceCode])
                    }
                }
                writeSymbol(bits, 256)
            }
            else {
                bits.write(final, 3)
                bits.align()
                output.writeShortLE(end - begin)
                output.writeShortLE((end - begin) xor 0xFFFF)
                output.write(data, begin, end - begin)
            }
        } while (position < data.size)
        bits.align()
    }

    override fun compress(data: ByteArray): ByteArray  {
        val output = ByteArray(maxCompressedSize(data.size))
        val size = compressPlaceable(data, output)
        return if (size == output.size) output else output.copyOf(size)
    }

    override fun compressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset) { encode(src, it) }

    private fun dynamicTrees(bits: DeflateBitReader): Pair<DeflateHuffman, DeflateHuffman> {
        val literalCount = bits.read(5) + 257
        val distanceCount = bits.read(5) + 1
        val codeCount = bits.read(4) + 4
        if (literalCount > 286) throw CompressionInvalidDataException("Invalid Deflate literal count", offset = bits.byteOffset)
        val codeLengths = IntArray(19)
        repeat(codeCount) { codeLengths[CODE_LENGTH_ORDER[it]] = bits.read(3) }
        val codeTree = DeflateHuffman(codeLengths, allowSingle = false)
        val lengths = IntArray(literalCount + distanceCount)
        var index = 0
        while (index < lengths.size) {
            val symbol = bits.symbol(codeTree)
            if (symbol <= 15) lengths[index++] = symbol
            else {
                val value: Int
                val count: Int
                when (symbol) {
                    16 -> {
                        if (index == 0) throw CompressionInvalidDataException("Huffman repeat has no previous length", offset = bits.byteOffset)
                        value = lengths[index - 1]
                        count = bits.read(2) + 3
                    }
                    17 -> {
                        value = 0
                        count = bits.read(3) + 3
                    }
                    else -> {
                        value = 0
                        count = bits.read(7) + 11
                    }
                }
                if (count > lengths.size - index) throw CompressionInvalidDataException("Huffman repeat exceeds the tree size", offset = bits.byteOffset)
                lengths.fill(value, index, index + count)
                index += count
            }
        }
        if (lengths[256] == 0) throw CompressionInvalidDataException("Deflate tree has no end-of-block code", offset = bits.byteOffset)
        return DeflateHuffman(lengths.copyOfRange(0, literalCount)) to DeflateHuffman(lengths.copyOfRange(literalCount, lengths.size), allowEmpty = true)
    }

    private fun decodeBlock(bits: DeflateBitReader, output: CompressionOutput, literals: DeflateHuffman, distances: DeflateHuffman, historyStart: Int, windowSize: Int) {
        while (true) {
            val symbol = bits.symbol(literals)
            when {
                symbol < 256 -> output.writeByte(symbol)
                symbol == 256 -> return
                symbol <= 285 -> {
                    val lengthCode = symbol - 257
                    val length = LENGTH_BASE[lengthCode] + bits.read(LENGTH_EXTRA[lengthCode])
                    val distanceCode = bits.symbol(distances)
                    if (distanceCode >= 30) throw CompressionInvalidDataException("Reserved Deflate distance code", offset = bits.byteOffset)
                    val distance = DISTANCE_BASE[distanceCode] + bits.read(DISTANCE_EXTRA[distanceCode])
                    if (distance > windowSize) throw CompressionInvalidDataException("Deflate distance exceeds the window size", offset = bits.byteOffset)
                    output.copyMatch(distance, length, historyStart)
                }
                else -> throw CompressionInvalidDataException("Reserved Deflate length code", offset = bits.byteOffset)
            }
        }
    }

    // 返回字节对齐后的结束位置，供 Zlib/Gzip 读取校验尾部；不消费后续成员。
    internal fun decode(data: ByteArray, output: CompressionOutput, start: Int = 0, end: Int = data.size, windowSize: Int = 32768): Int {
        val bits = DeflateBitReader(CompressionInput(data, start, end))
        val historyStart = output.size
        var final: Boolean
        do {
            final = bits.read(1) != 0
            when (bits.read(2)) {
                0 -> {
                    bits.align()
                    val length = bits.read(16)
                    if (bits.read(16) != (length xor 0xFFFF)) throw CompressionInvalidDataException("Stored block length check failed", offset = bits.byteOffset)
                    bits.copyTo(output, length)
                }
                1 -> decodeBlock(bits, output, FIXED_LITERALS, FIXED_DISTANCES, historyStart, windowSize)
                2 -> {
                    val [literals, distances] = dynamicTrees(bits)
                    decodeBlock(bits, output, literals, distances, historyStart, windowSize)
                }
                else -> throw CompressionInvalidDataException("Reserved Deflate block type", offset = bits.byteOffset)
            }
        } while (!final)
        bits.align()
        return bits.byteOffset
    }

    private fun decodeRaw(data: ByteArray, output: CompressionOutput) {
        val end = decode(data, output)
        if (end != data.size) throw CompressionInvalidDataException("Trailing data after Deflate stream", offset = end)
    }

    override fun decompress(data: ByteArray): ByteArray = CompressionOutput.decompressBytes(maxDecompressedSize) {
        decodeRaw(data, it)
    }

    override fun decompressPlaceable(src: ByteArray, dst: ByteArray, dstOffset: Int): Int = CompressionOutput.writeInto(src, dst, dstOffset, maxDecompressedSize) {
        decodeRaw(src, it)
    }
}
