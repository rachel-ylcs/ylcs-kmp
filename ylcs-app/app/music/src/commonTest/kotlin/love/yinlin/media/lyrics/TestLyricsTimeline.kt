package love.yinlin.media.lyrics

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLyricsTimeline {
    @Test
    fun duplicateTimestampsAndBoundaryPositionsSelectLastStartedLine() {
        val lines = [StaticLine(100, "a"), StaticLine(100, "translation"), StaticLine(200, "b")]
        val timeline = LyricsTimeline(lines)
        for ([position, expected] in [Long.MIN_VALUE to -1, 99L to -1, 100L to 1, 199L to 1, 200L to 2, Long.MAX_VALUE to 2]) {
            assertEquals(expected, timeline.indexAt(position))
        }
        assertEquals(-1, LyricsTimeline<StaticLine>([]).indexAt(Long.MAX_VALUE))
    }

    @Test
    fun randomSeeksMatchLinearReference() {
        val lines = List(10000) { StaticLine((it / 2 * 100).toLong(), "line $it") }
        val timeline = LyricsTimeline(lines)
        repeat(500) {
            val position = Random.nextLong(-1000, 600000)
            val firstLater = lines.indexOfFirst { it.position > position }
            assertEquals(if (firstLater == -1) lines.lastIndex else firstLater - 1, timeline.indexAt(position))
        }
    }

    @Test
    fun unsortedCustomLyricsKeepLinearScanSemantics() {
        val lines = [StaticLine(100, "a"), StaticLine(300, "b"), StaticLine(200, "c")]
        val timeline = LyricsTimeline(lines)
        for (position in [0L, 100L, 200L, 250L, 300L, Long.MAX_VALUE]) {
            val firstLater = lines.indexOfFirst { it.position > position }
            assertEquals(if (firstLater == -1) lines.lastIndex else firstLater - 1, timeline.indexAt(position))
        }
    }
}