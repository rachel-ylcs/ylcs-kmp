package love.yinlin.foundation.parser.lrc

import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LrcTest {
    companion object {
        private const val SOURCE = """
            [00:17.03]謇傲花瓣在烈火中惊艳成灰
            [00:23.67]灼烫的夜凝视某些虚伪
            [00:30.57]敢在世俗唇舌上奔跑的明媚
            [00:36.94]该被倾慕也应该被敬畏
            [00:44.86]如同刀尖下挣扎着流水
            [00:51.51]潺潺淌出倔强的慈悲
            [00:58.15]也憧憬与大雪一同跌坠
            [01:03.51]激烈自由地被风包围
            [01:10.15]宁愿我凋谢成枯岩余晖
            [01:17.06]襟怀万里山海鼎沸
            [01:23.44]向人间烟火种半碗惊雷
            [01:30.60]埋在春风的韵尾
            [01:37.23]晨曦照彻白云碑
            [01:57.24]七情六欲还没疯完先别追悔
            [02:03.63]三千世界足够耗尽轮回
            [02:10.53]穷途末路就折了傲骨听玉碎
            [02:16.91]逆光的梦越真挚越垂危
            [02:24.58]情有独钟的是轻狂年岁
            [02:31.21]独上高楼宣苍龙来会
            [02:38.12]我爱的恨的众生都很美
            [02:43.49]缠绵过眼中每一滴泪
            [02:50.11]宁愿我凋谢成枯岩余晖
            [02:57.26]襟怀万里山海鼎沸
            [03:03.40]向人间烟火种半碗惊雷
            [03:10.55]埋在春风的韵尾
            [03:17.20]晨曦照彻白云碑
            [03:50.14]将绽放在最辽阔的明天
            [03:57.05]对峙心头那片荒原
            [04:03.44]我生就绚丽短暂如热恋
            [04:10.60]只想温柔地赴险
            [04:17.24]举世无双一千遍
        """.trimIndent()
    }

    private val expectedPositions: List<Long> = [
        17030L, 23670L, 30570L, 36940L, 44860L, 51510L, 58150L,
        63510L, 70150L, 77060L, 83440L, 90600L, 97230L, 117240L,
        123630L, 130530L, 136910L, 144580L, 151210L, 158120L,
        163490L, 170110L, 177260L, 183400L, 190550L, 197200L,
        230140L, 237050L, 243440L, 250600L, 257240L
    ]

    private val expectedLines = expectedPositions.zip(SOURCE.lines()) { position, line ->
        LrcLine(position, line.substringAfter(']'))
    }

    @Test
    fun parseSampleLyrics() {
        val lrc = Lrc.parse(SOURCE)

        assertEquals(31, lrc.lines.size)
        assertEquals(expectedLines, lrc.lines)
        assertEquals(LrcMetadata(), lrc.metadata)
        assertFalse(lrc.isEmpty)
        assertTrue(lrc.isNotEmpty)
    }

    @Test
    fun plainTextContainsOnlyLyrics() {
        val expected = SOURCE.lines().joinToString("\n") { it.substringAfter(']') }

        assertEquals(expected, Lrc.parse(SOURCE).plainText)
    }

    @Test
    fun writeSampleLyricsWithTwoDigitFractions() {
        assertEquals(SOURCE, Lrc.parse(SOURCE).toString())
    }

    @Test
    fun sampleLyricsRoundTripIsStable() {
        val expected = Lrc(expectedLines, LrcMetadata())
        var lrc = expected

        repeat(3) {
            assertEquals(SOURCE, lrc.toString())
            lrc = Lrc.parse(lrc.toString())
            assertEquals(expected, lrc)
        }
    }

    @Test
    fun lineEndingsAndSurroundingWhitespaceAreAccepted() {
        for (newline in ["\n", "\r\n", "\r"]) {
            val padded = SOURCE.lines().joinToString(newline + newline) { " \t$it \t" }
            val lrc = Lrc.parse(newline + padded + newline)

            assertEquals(expectedLines, lrc.lines, "newline=$newline")
            assertEquals(SOURCE, lrc.toString())
        }
    }

    @Test
    fun outOfOrderAndRepeatedLinesAreNormalized() {
        val shuffled = buildList {
            addAll(SOURCE.lines().reversed())
            addAll(SOURCE.lines().take(3))
            add(SOURCE.lines().last())
        }
        val lrc = Lrc.parse(shuffled.joinToString("\n"))

        assertEquals(expectedLines, lrc.lines)
        assertEquals(SOURCE, lrc.toString())
    }

    @Test
    fun multipleTimeTagsExpandWithoutLosingDifferentLyricsAtTheSameTime() {
        val tagged = SOURCE.replaceFirst("[00:17.03]", "[00:23.67][00:17.03][00:17.03]")
        val expected = expectedLines.toMutableList().apply {
            add(1, expectedLines.first().copy(position = 23670L))
        }
        val lrc = Lrc.parse(tagged)

        assertEquals(32, lrc.lines.size)
        assertEquals(expected, lrc.lines)
        assertEquals(lrc, Lrc.parse(lrc.toString()))
    }

    @Test
    fun allMetadataFieldsAreParsedAndWritten() {
        val header = """
            [ti:示例歌词]
            [ar:示例歌手]
            [al:测试专辑]
            [au:测试作者]
            [by:测试录入者]
            [re:测试编辑器]
            [ve:1.0]
            [length:04:17.24]
        """.trimIndent()
        val metadata = LrcMetadata(
            title = "示例歌词",
            artist = "示例歌手",
            album = "测试专辑",
            author = "测试作者",
            by = "测试录入者",
            editor = "测试编辑器",
            version = "1.0",
            length = "04:17.24"
        )
        val lrc = Lrc.parse("$header\n$SOURCE")

        assertEquals(metadata, lrc.metadata)
        assertEquals(expectedLines, lrc.lines)
        assertEquals("$header\n$SOURCE", lrc.toString())
        assertEquals(lrc, Lrc.parse(lrc.toString()))
    }

    @Test
    fun metadataKeysIgnoreCaseAndTheLastValueWins() {
        val lrc = Lrc.parse("[ti:旧标题]\n[TI: 新标题 ]\n[Ar: 示例歌手 ]\n[unknown:忽略]\n$SOURCE\n[Ti: 最终标题 ]")

        assertEquals(LrcMetadata(title = "最终标题", artist = "示例歌手"), lrc.metadata)
        assertEquals(expectedLines, lrc.lines)
        assertEquals("[ti:最终标题]\n[ar:示例歌手]\n$SOURCE", lrc.toString())
    }

    @Test
    fun blankMetadataIsIgnoredAndOmittedWhenWriting() {
        val parsed = Lrc.parse("[ti:]\n[ar:   ]\n[al: \t ]\n$SOURCE")
        val constructed = Lrc(expectedLines, LrcMetadata(title = "", artist = "   ", album = "\t"))

        assertEquals(LrcMetadata(), parsed.metadata)
        assertEquals(expectedLines, parsed.lines)
        assertEquals(SOURCE, parsed.toString())
        assertEquals(SOURCE, constructed.toString())
    }

    @Test
    fun positiveAndNegativeOffsetsRoundTripWithoutAccumulating() {
        for (offset in [125L, -125L, 1000L, -1000L]) {
            val input = "[offset:$offset]\n$SOURCE"
            val expected = Lrc(
                expectedLines.map { it.copy(position = it.position + offset) },
                LrcMetadata(offset = offset)
            )
            var lrc = Lrc.parse(input)

            repeat(3) {
                assertEquals(expected, lrc, "offset=$offset")
                assertEquals(input, lrc.toString(), "offset=$offset")
                lrc = Lrc.parse(lrc.toString())
            }
            assertEquals(expected, lrc, "offset=$offset")
        }
    }

    @Test
    fun negativeOffsetsClampTimesToZeroWithoutDroppingDifferentLyrics() {
        val lrc = Lrc.parse("[offset:-30000]\n$SOURCE")

        assertEquals(31, lrc.lines.size)
        assertEquals([0L, 0L, 570L], lrc.lines.take(3).map { it.position })
        assertEquals(227240L, lrc.lines.last().position)
        assertEquals(expectedLines.map { it.text }, lrc.lines.map { it.text })
        assertEquals(-30000L, lrc.metadata.offset)
        assertEquals(lrc, Lrc.parse(lrc.toString()))
    }

    @Test
    fun offsetsAfterLyricsAndRepeatedOffsetTagsUseTheLastValue() {
        val expected = Lrc(
            expectedLines.map { it.copy(position = it.position - 125L) },
            LrcMetadata(offset = -125L)
        )

        assertEquals(expected, Lrc.parse("$SOURCE\n[offset:-125]"))
        assertEquals(expected, Lrc.parse("[offset:1000]\n$SOURCE\n[offset:-125]"))
    }

    @Test
    fun invalidOffsetsFallBackToZero() {
        for (offset in ["", "invalid", "1.5", "9223372036854775808", "-9223372036854775809"]) {
            val lrc = Lrc.parse("[offset:$offset]\n$SOURCE")

            assertEquals(LrcMetadata(), lrc.metadata, "offset=$offset")
            assertEquals(expectedLines, lrc.lines, "offset=$offset")
            assertEquals(SOURCE, lrc.toString())
        }
    }

    @Test
    fun emptyAndUntimedSourcesProduceEmptyLyrics() {
        for (input in ["", " \t\r\n ", "没有时间戳的歌词", "[00:01.00]", "[00:02.00]   ", "[unknown:忽略]"]) {
            val lrc = Lrc.parse(input)

            assertTrue(lrc.isEmpty, input)
            assertFalse(lrc.isNotEmpty, input)
            assertEquals(emptyList(), lrc.lines, input)
            assertEquals(LrcMetadata(), lrc.metadata, input)
            assertEquals("", lrc.plainText, input)
            assertEquals("", lrc.toString(), input)
        }
    }

    @Test
    fun metadataOnlyLyricsCanRoundTrip() {
        val lrc = Lrc.parse("[ti:示例歌词]\n[offset:125]")

        assertTrue(lrc.isEmpty)
        assertFalse(lrc.isNotEmpty)
        assertEquals("", lrc.plainText)
        assertEquals(LrcMetadata(title = "示例歌词", offset = 125L), lrc.metadata)
        assertEquals("[ti:示例歌词]\n[offset:125]", lrc.toString().trimEnd('\n'))
        assertEquals(lrc, Lrc.parse(lrc.toString()))
    }

    @Test
    fun invalidTimeTagsAreIgnoredWithoutAffectingValidLyrics() {
        for (tag in [
            "[00:60.00]", "[00:99.00]", "[-01:02.00]", "[00:-01.00]",
            "[00:01.1]", "[00:01.1234]", "[00:01.]", "[00:01.00",
            "[x:01.00]", "[9223372036854775808:00.00]"
        ]) {
            val invalid = "${tag}无效歌词"

            assertTrue(Lrc.parse(invalid).isEmpty, tag)
            assertEquals(expectedLines, Lrc.parse("$invalid\n$SOURCE").lines, tag)
        }
    }

    @Test
    fun supportedTimestampVariantsParseWithTheCorrectPrecision() {
        for ([timestamp, position] in [
            "[0:0]" to 0L,
            "[1:2]" to 62000L,
            "[01:02.03]" to 62030L,
            "[01:02:03]" to 62030L,
            "[01:02.003]" to 62003L,
            "[01:02:003]" to 62003L,
            "[123:45.67]" to 7425670L
        ]) {
            assertEquals([LrcLine(position, "边界歌词")], Lrc.parse("${timestamp}边界歌词").lines, timestamp)
        }
    }

    @Test
    fun writerTruncatesToHundredthsAndPadsTimeParts() {
        for ([position, timestamp] in [
            -1L to "[00:00.00]",
            0L to "[00:00.00]",
            1L to "[00:00.00]",
            9L to "[00:00.00]",
            10L to "[00:00.01]",
            99L to "[00:00.09]",
            100L to "[00:00.10]",
            999L to "[00:00.99]",
            1000L to "[00:01.00]",
            59999L to "[00:59.99]",
            60000L to "[01:00.00]",
            3599999L to "[59:59.99]",
            3600000L to "[60:00.00]",
            6000000L to "[100:00.00]"
        ]) {
            val lrc = Lrc([LrcLine(position, "边界歌词")], LrcMetadata())

            assertEquals("${timestamp}边界歌词", lrc.toString(), "position=$position")
        }
    }

    @Test
    fun serializeLrc() {
        val lrc = Lrc.parse(SOURCE)
        val jsonString = lrc.toJsonString()
        val decodeLrc = jsonString.parseJsonValue<Lrc>()
        assertEquals(lrc, decodeLrc)
    }
}
