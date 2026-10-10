package love.yinlin.encoding

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class EncodingTest {
    private fun supplementary(code: Int): String = charArrayOf(
        (0xD800 + ((code - 0x10000) shr 10)).toChar(),
        (0xDC00 + ((code - 0x10000) and 0x3FF)).toChar(),
    ).concatToString()

    private fun crc32Byte(checksum: Int, value: Int): Int {
        var result = checksum xor (value and 0xFF)
        repeat(8) { result = (result ushr 1) xor if (result and 1 == 0) 0 else 0xEDB88320.toInt() }
        return result
    }

    private fun hex(value: String): ByteArray = value.hexToByteArray(HexFormat.UpperCase)

    private val unicodeEncodings = [Encoding.UTF16LE, Encoding.UTF16BE, Encoding.UTF32LE, Encoding.UTF32BE]

    private fun crc32(bytes: ByteArray): Int {
        var checksum = -1
        for (byte in bytes) checksum = crc32Byte(checksum, byte.toInt())
        return checksum.inv()
    }

    private fun reverseUnits(bytes: ByteArray, width: Int): ByteArray {
        val result = bytes.copyOf()
        val end = bytes.size - bytes.size % width
        for (start in 0 ..< end step width) {
            for (offset in 0 ..< width) result[start + offset] = bytes[start + width - offset - 1]
        }
        return result
    }

    @Test
    fun testEmptyAndAscii() {
        val bytes = ByteArray(128) { it.toByte() }
        val text = CharArray(128) { it.toChar() }.concatToString()
        for (encoding in Encoding.entries) {
            assertContentEquals([], "".convert(encoding))
            assertEquals("", byteArrayOf().convert(encoding))
        }
        for (encoding in [Encoding.UTF8, Encoding.ASCII, Encoding.LATIN1, Encoding.GBK]) {
            assertContentEquals(bytes, text.convert(encoding))
            assertEquals(text, bytes.convert(encoding))
        }
    }

    @Test
    fun testASCIIEveryByteValue() {
        val bytes = ByteArray(256) { it.toByte() }
        val ascii = CharArray(128) { it.toChar() }.concatToString()
        assertEquals(ascii + "\uFFFD".repeat(128), bytes.convert(Encoding.ASCII))
    }

    @Test
    fun testASCIIUnmappableBmpCharacters() {
        val text = "A\u007F\u0080é中€\uFFFD\uFFFFZ"
        val bytes = hex("417F3F3F3F3F3F3F5A")
        assertContentEquals(bytes, text.convert(Encoding.ASCII))
        assertEquals("A\u007F??????Z", bytes.convert(Encoding.ASCII))
    }

    @Test
    fun testASCIIInvalidBytesPreserveText() {
        val cases = [
            "00807FFF" to "\u0000\uFFFD\u007F\uFFFD",
            "41C28042" to "A\uFFFD\uFFFDB",
            "41E4B8AD42" to "A\uFFFD\uFFFD\uFFFDB",
            "41F09F988042" to "A\uFFFD\uFFFD\uFFFD\uFFFDB",
        ]
        for ([bytes, text] in cases) assertEquals(text, convertASCIIToString(hex(bytes)), bytes)
    }

    @Test
    fun testASCIISurrogates() {
        val cases = [
            "\uD800" to "3F",
            "\uDC00" to "3F",
            "\uD800\uDC00" to "3F",
            "\uDBFF\uDFFF" to "3F",
            "A\uD83D\uDE00B" to "413F42",
            "\uD800A\uDC00" to "3F413F",
            "\uD800\uD800" to "3F3F",
            "\uDC00\uDC00" to "3F3F",
            "\uDC00\uD800" to "3F3F",
            "\uD800\uD800\uDC00" to "3F3F",
            "\uDC00\uD800\uDC00\uDBFF" to "3F3F3F",
            "\uD83D\uDE00\uD83C\uDF0DZ" to "3F3F5A",
        ]
        for ([text, bytes] in cases) {
            assertContentEquals(hex(bytes), convertStringToASCII(text), bytes)
        }
    }

    @Test
    fun testASCIISupplementaryPlanes() {
        for (code in 0x10000 .. 0x10FFFF step 997) {
            assertContentEquals([0x3F], convertStringToASCII(supplementary(code)), "U+${code.toString(16)}")
        }
    }

    @Test
    fun testLatin1EveryByteValue() {
        val bytes = ByteArray(256) { it.toByte() }
        val text = CharArray(256) { it.toChar() }.concatToString()
        assertEquals(text, bytes.convert(Encoding.LATIN1))
        assertContentEquals(bytes, text.convert(Encoding.LATIN1))
    }

    @Test
    fun testLatin1WesternText() {
        val text = "café, Noël, Straße, £5"
        val bytes = hex("636166E92C204E6FEB6C2C2053747261DF652C20A335")
        assertContentEquals(bytes, text.convert(Encoding.LATIN1))
        assertEquals(text, bytes.convert(Encoding.LATIN1))
    }

    @Test
    fun testLatin1UnmappableBmpCharacters() {
        val text = "\u0000\u007F\u0080ÿ\u0100\u017F中€\uFFFD\uFFFFé"
        val bytes = hex("007F80FF3F3F3F3F3F3FE9")
        assertContentEquals(bytes, text.convert(Encoding.LATIN1))
        assertEquals("\u0000\u007F\u0080ÿ??????é", bytes.convert(Encoding.LATIN1))
    }

    @Test
    fun testLatin1ControlBytesAndUtf8Sequences() {
        val cases = [
            "80919293949F" to "\u0080\u0091\u0092\u0093\u0094\u009F",
            "C3A9" to "\u00C3\u00A9",
            "E4B8AD" to "\u00E4\u00B8\u00AD",
            "F09F9880" to "\u00F0\u009F\u0098\u0080",
        ]
        for ([bytes, text] in cases) {
            assertEquals(text, hex(bytes).convert(Encoding.LATIN1), bytes)
            assertContentEquals(hex(bytes), text.convert(Encoding.LATIN1), bytes)
        }
        assertContentEquals(hex("3F3F3F3F3F3F"), "€‘’“”Ÿ".convert(Encoding.LATIN1))
    }

    @Test
    fun testLatin1Surrogates() {
        val cases = [
            "\uD800" to "3F",
            "\uDC00" to "3F",
            "\uD800\uDC00" to "3F",
            "\uDBFF\uDFFF" to "3F",
            "é\uD83D\uDE00ÿ" to "E93FFF",
            "\uD800é\uDC00" to "3FE93F",
            "\uD800\uD800" to "3F3F",
            "\uDC00\uDC00" to "3F3F",
            "\uDC00\uD800" to "3F3F",
            "\uD800\uD800\uDC00" to "3F3F",
            "\uDC00\uD800\uDC00\uDBFF" to "3F3F3F",
            "\uD83D\uDE00\uD83C\uDF0Dé" to "3F3FE9",
        ]
        for ([text, bytes] in cases) {
            assertContentEquals(hex(bytes), text.convert(Encoding.LATIN1), bytes)
        }
    }

    @Test
    fun testLatin1SupplementaryPlanes() {
        for (code in 0x10000 .. 0x10FFFF step 997) {
            assertContentEquals([0x3F], convertStringToLatin1(supplementary(code)), "U+${code.toString(16)}")
        }
    }

    @Test
    fun testGBKMappingRegionsAndEuro() {
        val text = "你好，世界！GBK丂亍龥\uFA29\u2295"
        val bytes = hex("C4E3BAC3A3ACCAC0BDE7A3A147424B8140D8A1FD9BFE4FA892")
        assertContentEquals(bytes, text.convert(Encoding.GBK))
        assertEquals(text, bytes.convert(Encoding.GBK))
        assertContentEquals(hex("804180"), "€A€".convert(Encoding.GBK))
        assertEquals("€A€", hex("804180").convert(Encoding.GBK))
    }

    @Test
    fun testGBKMalformedInputPreservesAscii() {
        val cases = listOf(
            "81" to "\uFFFD",
            "FF" to "\uFFFD",
            "8130" to "\uFFFD0",
            "817F" to "\uFFFD\u007F",
            "A140" to "\uFFFD@",
            "A180" to "\uFFFD",
            "81FF41" to "\uFFFD\uFFFDA",
            "FF80" to "\uFFFD€",
            "90308130" to "\uFFFD0\uFFFD0",
        )
        for ([r, v] in cases) assertEquals(v, convertGBKToString(hex(r)), r)
    }

    @Test
    fun testGBKUnmappableCharactersAndSurrogates() {
        val text = "A\uD83D\uDE00\uD800中\uDC00\uD800\uD800\uDC00\uE000Z"
        assertContentEquals(hex("413F3FD6D03F3F3F3F5A"), convertStringToGBK(text))
        assertContentEquals([0x3F], convertStringToGBK("\u2641"))
    }

    @Test
    fun testGBKEveryDoubleByteSlot() {
        var checksum = -1
        var assigned = 0
        val corpus = StringBuilder()
        val corpusBytes = ByteArray(21791 * 2)
        for (lead in 0x81..0xFE) {
            for (trail in 0x40..0xFE) {
                if (trail == 0x7F) continue
                val bytes: ByteArray = [lead.toByte(), trail.toByte()]
                val text = convertGBKToString(bytes)
                val code = if (text.length == 1 && text[0] != '\uFFFD') {
                    assertContentEquals(bytes, convertStringToGBK(text), "$lead/$trail")
                    corpus.append(text)
                    corpusBytes[assigned * 2] = lead.toByte()
                    corpusBytes[assigned * 2 + 1] = trail.toByte()
                    assigned++
                    text[0].code
                }
                else {
                    val expected = if (trail < 0x80) "\uFFFD" + trail.toChar() else "\uFFFD"
                    assertEquals(expected, text, "$lead/$trail")
                    0
                }
                checksum = crc32Byte(crc32Byte(checksum, code), code shr 8)
            }
        }
        assertEquals(21791, assigned)
        assertEquals(0xD6965DC7.toInt(), checksum.inv())
        assertContentEquals(corpusBytes, convertStringToGBK(corpus.toString()))
        assertEquals(corpus.toString(), convertGBKToString(corpusBytes))
    }

    @Test
    fun testGBKEveryBmpCodeUnit() {
        var checksum = -1
        for (code in 0 .. 0xFFFF) {
            val bytes = convertStringToGBK(code.toChar().toString())
            checksum = crc32Byte(checksum, bytes.size)
            for (byte in bytes) checksum = crc32Byte(checksum, byte.toInt())
        }
        assertEquals(0xD34A151C.toInt(), checksum.inv())
    }

    @Test
    fun testUTF16ByteOrderAndBoundaries() {
        val text = "\u0000\u007F\u0080\u07FF\u0800\uD7FF\uE000\uFEFF\uFFFE\uFFFF\uD800\uDC00\uDBFF\uDFFF"
        val cases = [
            Encoding.UTF16LE to "00007F008000FF070008FFD700E0FFFEFEFFFFFF00D800DCFFDBFFDF",
            Encoding.UTF16BE to "0000007F008007FF0800D7FFE000FEFFFFFEFFFFD800DC00DBFFDFFF",
        ]
        for ([encoding, value] in cases) {
            val bytes = hex(value)
            assertContentEquals(bytes, text.convert(encoding), encoding.name)
            assertEquals(text, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUTF32ByteOrderAndBoundaries() {
        val text = "\u0000\u007F\u0080\u07FF\u0800\uD7FF\uE000\uFEFF\uFFFE\uFFFF\uD800\uDC00\uDBFF\uDFFF"
        val cases = [
            Encoding.UTF32LE to "000000007F00000080000000FF07000000080000FFD7000000E00000FFFE0000FEFF0000FFFF000000000100FFFF1000",
            Encoding.UTF32BE to "000000000000007F00000080000007FF000008000000D7FF0000E0000000FEFF0000FFFE0000FFFF000100000010FFFF",
        ]
        for ([encoding, value] in cases) {
            val bytes = hex(value)
            assertContentEquals(bytes, text.convert(encoding), encoding.name)
            assertEquals(text, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUTF16EveryValidBmpCodeUnit() {
        val text = buildString {
            for (code in 0 .. 0xFFFF) if (code !in 0xD800 .. 0xDFFF) append(code.toChar())
        }
        val cases = [Encoding.UTF16LE to 0x2300E2A3, Encoding.UTF16BE to 0x0C7AAD0E]
        for ([encoding, checksum] in cases) {
            val bytes = text.convert(encoding)
            assertEquals(126976, bytes.size, encoding.name)
            assertEquals(checksum, crc32(bytes), encoding.name)
            assertEquals(text, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUTF32EveryValidBmpCodeUnit() {
        val text = buildString {
            for (code in 0 .. 0xFFFF) if (code !in 0xD800 .. 0xDFFF) append(code.toChar())
        }
        val cases = [Encoding.UTF32LE to 0x4290C0D9, Encoding.UTF32BE to 0x032F428A]
        for ([encoding, checksum] in cases) {
            val bytes = text.convert(encoding)
            assertEquals(253952, bytes.size, encoding.name)
            assertEquals(checksum, crc32(bytes), encoding.name)
            assertEquals(text, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUTF16MalformedInput() {
        val cases = [
            "00" to "\uFFFD",
            "FF" to "\uFFFD",
            "D800" to "\uFFFD",
            "DBFF" to "\uFFFD",
            "DC00" to "\uFFFD",
            "DFFF" to "\uFFFD",
            "D8000041" to "\uFFFDA",
            "DC000041" to "\uFFFDA",
            "D800D800" to "\uFFFD\uFFFD",
            "DC00DC00" to "\uFFFD\uFFFD",
            "DC00D800" to "\uFFFD\uFFFD",
            "D800D800DC00" to "\uFFFD\uD800\uDC00",
            "D8000041DC00" to "\uFFFDA\uFFFD",
            "0041DC000042" to "A\uFFFDB",
            "004100" to "A\uFFFD",
            "D80000" to "\uFFFD\uFFFD",
            "D83DDE0000" to "\uD83D\uDE00\uFFFD",
            "0041D83DDE00DBFF0042" to "A\uD83D\uDE00\uFFFDB",
        ]
        for ([value, text] in cases) {
            val bytes = hex(value)
            assertEquals(text, bytes.convert(Encoding.UTF16BE), value)
            assertEquals(text, reverseUnits(bytes, 2).convert(Encoding.UTF16LE), value)
        }
    }

    @Test
    fun testUTF32MalformedInput() {
        val cases = [
            "00" to "\uFFFD",
            "0000" to "\uFFFD",
            "000000" to "\uFFFD",
            "FFFFFF" to "\uFFFD",
            "0000D800" to "\uFFFD",
            "0000DBFF" to "\uFFFD",
            "0000DC00" to "\uFFFD",
            "0000DFFF" to "\uFFFD",
            "00110000" to "\uFFFD",
            "01000041" to "\uFFFD",
            "7FFFFFFF" to "\uFFFD",
            "80000000" to "\uFFFD",
            "FFFFFFFF" to "\uFFFD",
            "0000D8000000DC00" to "\uFFFD\uFFFD",
            "0000D80000000041" to "\uFFFDA",
            "8000000000000041" to "\uFFFDA",
            "001100000000FFFF" to "\uFFFD\uFFFF",
            "0000004100" to "A\uFFFD",
            "000000410000" to "A\uFFFD",
            "00000041000000" to "A\uFFFD",
            "000000410001F600FFFFFFFF00000042" to "A\uD83D\uDE00\uFFFDB",
        ]
        for ([value, text] in cases) {
            val bytes = hex(value)
            assertEquals(text, bytes.convert(Encoding.UTF32BE), value)
            assertEquals(text, reverseUnits(bytes, 4).convert(Encoding.UTF32LE), value)
        }
    }

    @Test
    fun testUnicodeUnpairedSurrogates() {
        val cases = [
            "\uD800" to "\uFFFD",
            "\uDBFF" to "\uFFFD",
            "\uDC00" to "\uFFFD",
            "\uDFFF" to "\uFFFD",
            "\uD800\uDC00" to "\uD800\uDC00",
            "\uDBFF\uDFFF" to "\uDBFF\uDFFF",
            "é\uD83D\uDE00中" to "é\uD83D\uDE00中",
            "\uD800A\uDC00" to "\uFFFDA\uFFFD",
            "\uD800\uD800" to "\uFFFD\uFFFD",
            "\uDC00\uDC00" to "\uFFFD\uFFFD",
            "\uDC00\uD800" to "\uFFFD\uFFFD",
            "\uD800\uD800\uDC00" to "\uFFFD\uD800\uDC00",
            "\uDC00\uD800\uDC00\uDBFF" to "\uFFFD\uD800\uDC00\uFFFD",
        ]
        for (encoding in unicodeEncodings) {
            for ([text, expected] in cases) {
                val bytes = text.convert(encoding)
                assertContentEquals(expected.convert(encoding), bytes, encoding.name)
                assertEquals(expected, bytes.convert(encoding), encoding.name)
            }
        }
        val text = buildString {
            for (code in 0xD800 .. 0xDFFF) append(code.toChar()).append('A')
        }
        val expected = "\uFFFDA".repeat(2048)
        for (encoding in unicodeEncodings) {
            val bytes = text.convert(encoding)
            assertContentEquals(expected.convert(encoding), bytes, encoding.name)
            assertEquals(expected, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUnicodeSupplementaryPlanes() {
        val text = buildString {
            for (code in 0x10000 .. 0x10FFFF step 997) append(supplementary(code))
        }
        val cases = [
            Encoding.UTF16LE to 0x65F52E37,
            Encoding.UTF16BE to 0x1DF96275,
            Encoding.UTF32LE to 0xD65A2850.toInt(),
            Encoding.UTF32BE to 0x24C28727,
        ]
        for ([encoding, checksum] in cases) {
            val bytes = text.convert(encoding)
            assertEquals(4208, bytes.size, encoding.name)
            assertEquals(checksum, crc32(bytes), encoding.name)
            assertEquals(text, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUnicodeByteOrderMarkIsContent() {
        val text = "\uFEFFA\uFEFF\uFFFE"
        val cases = [
            Encoding.UTF16LE to "FFFE4100FFFEFEFF",
            Encoding.UTF16BE to "FEFF0041FEFFFFFE",
            Encoding.UTF32LE to "FFFE000041000000FFFE0000FEFF0000",
            Encoding.UTF32BE to "0000FEFF000000410000FEFF0000FFFE",
        ]
        for ([encoding, value] in cases) {
            val bytes = hex(value)
            assertContentEquals(bytes, text.convert(encoding), encoding.name)
            assertEquals(text, bytes.convert(encoding), encoding.name)
        }
    }

    @Test
    fun testUnicodeMixedTextRoundTrips() {
        val random = Random(1632)
        repeat(100) {
            val text = buildString {
                repeat(128) {
                    when (random.nextInt(3)) {
                        0 -> append(random.nextInt(128).toChar())
                        1 -> {
                            val code = random.nextInt(0xF800)
                            append((if (code >= 0xD800) code + 0x800 else code).toChar())
                        }
                        else -> append(supplementary(random.nextInt(0x10000, 0x110000)))
                    }
                }
            }
            for (encoding in unicodeEncodings) {
                val bytes = text.convert(encoding)
                val size = when (encoding) {
                    Encoding.UTF16LE, Encoding.UTF16BE -> text.length * 2
                    else -> 128 * 4
                }
                assertEquals(size, bytes.size, encoding.name)
                assertEquals(text, bytes.convert(encoding), encoding.name)
            }
        }
    }

    @Test
    fun testUTF8WidthBoundaries() {
        val text = "\u0000\u007F\u0080\u07FF\u0800\uD7FF\uE000\uFFFF\uD800\uDC00\uDBFF\uDFFF"
        val bytes = hex("007FC280DFBFE0A080ED9FBFEE8080EFBFBFF0908080F48FBFBF")
        assertContentEquals(bytes, convertStringToUTF8(text))
        assertEquals(text, convertUTF8ToString(bytes))
    }

    @Test
    fun testUTF8MultilingualText() {
        val text = "中文 日本語 한국어 Ελληνικά русский العربية English \uD83D\uDE00\uD83C\uDF0D\n\u0000"
        assertContentEquals(text.encodeToByteArray(), text.convert(Encoding.UTF8))
        assertEquals(text, text.convert(Encoding.UTF8).convert(Encoding.UTF8))
        val chinese = hex("E4BDA0E5A5BDEFBC8CE4B896E7958CEFBC81")
        assertEquals("你好，世界！", convertUTF8ToString(chinese))
    }

    @Test
    fun testUTF8EveryValidBmpCodeUnit() {
        for (code in 0 .. 0xFFFF) {
            if (code in 0xD800 .. 0xDFFF) continue
            val text = code.toChar().toString()
            val expected = text.encodeToByteArray()
            assertContentEquals(expected, convertStringToUTF8(text), "U+${code.toString(16)}")
            assertEquals(text, convertUTF8ToString(expected), "U+${code.toString(16)}")
        }
    }

    @Test
    fun testUTF8SupplementaryPlanes() {
        for (code in 0x10000 .. 0x10FFFF step 997) {
            val text = supplementary(code)
            val expected = text.encodeToByteArray()
            assertContentEquals(expected, convertStringToUTF8(text), "U+${code.toString(16)}")
            assertEquals(text, convertUTF8ToString(expected), "U+${code.toString(16)}")
        }
    }

    @Test
    fun testUTF8EverySingleAndDoubleByteInput() {
        for (first in 0 .. 0xFF) {
            val single: ByteArray = [first.toByte()]
            assertEquals(single.decodeToString(), convertUTF8ToString(single), "$first")
            for (second in 0 .. 0xFF) {
                val bytes: ByteArray = [first.toByte(), second.toByte()]
                assertEquals(bytes.decodeToString(), convertUTF8ToString(bytes), "$first/$second")
            }
        }
    }

    @Test
    fun testUTF8MalformedSequences() {
        val cases = [
            "FF", "C0AF", "E08080", "EDA080", "EDBFBF", "F0808080", "F4908080", "F5808080",
            "C2", "E0A0", "E282", "F090", "F09F92", "F09080",
            "C241", "E0A041", "E28241", "F09041", "F0908041", "E228A1", "EFBBBF41",
        ]
        for (value in cases) {
            val bytes = hex(value)
            assertEquals(bytes.decodeToString(), convertUTF8ToString(bytes), value)
        }
        assertEquals("\uFFFDA", convertUTF8ToString(hex("E28241")))
        assertEquals("\uFFFDA", convertUTF8ToString(hex("F0908041")))
    }

    @Test
    fun testUTF8UnpairedSurrogates() {
        val cases = [
            "\uD800", "\uDC00", "\uD800A\uDC00", "\uD800\uD800\uDC00",
            "\uDC00\uD83D\uDE00\uD800", "A\uD83D\uDE00Z",
        ]
        for (text in cases) {
            val expected = text.encodeToByteArray()
            assertContentEquals(expected, convertStringToUTF8(text))
            assertEquals(expected.decodeToString(), convertUTF8ToString(convertStringToUTF8(text)))
        }
    }

    @Test
    fun testUTF8MixedUnicodeRoundTrips() {
        val random = Random(936)
        repeat(300) {
            val text = buildString {
                repeat(128) {
                    when (random.nextInt(3)) {
                        0 -> append(random.nextInt(128).toChar())
                        1 -> {
                            val code = random.nextInt(0xF800)
                            append((if (code >= 0xD800) code + 0x800 else code).toChar())
                        }
                        else -> append(supplementary(random.nextInt(0x10000, 0x110000)))
                    }
                }
            }
            val expected = text.encodeToByteArray()
            assertContentEquals(expected, convertStringToUTF8(text))
            assertEquals(text, convertUTF8ToString(convertStringToUTF8(text)))
        }
    }
}
