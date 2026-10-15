package love.yinlin.foundation.fts

import kotlin.math.abs

/** 带状 Levenshtein 动态规划：每行最多 2 * distance + 1 个单元，供 Trie 和大小写核验共用。 */
internal class EditDistance(
    private val query: String,
    private val distance: Int
) {
    val width: Int = minOf(query.length.toLong() + 1, distance.toLong() * 2 + 1).toInt()
    private val infinity = if (distance == Int.MAX_VALUE) distance else distance + 1
    private var scratch: IntArray? = null

    fun initial(): IntArray = IntArray(width) { it }

    private fun start(depth: Int): Int = maxOf(0, depth - distance)
    private fun end(depth: Int): Int = if (distance >= query.length - depth) query.length else depth + distance
    private fun value(row: IntArray, offset: Int, column: Int, start: Int, end: Int): Int = if (column in start .. end) row[offset + column - start] else infinity
    private fun increment(value: Int): Int = if (value >= infinity) infinity else value + 1

    fun advance(char: Char, depth: Int, rows: IntArray, previousOffset: Int, offset: Int): Int {
        val from = start(depth)
        val to = end(depth)
        val previousStart = start(depth - 1)
        val previousEnd = end(depth - 1)
        var minimum = infinity
        for (column in from..to) {
            val current = if (column == 0) depth else {
                val diagonal = value(rows, previousOffset, column - 1, previousStart, previousEnd)
                minOf(
                    increment(value(rows, previousOffset, column, previousStart, previousEnd)),
                    if (column == from) infinity else increment(rows[offset + column - from - 1]),
                    if (query[column - 1] == char) diagonal else increment(diagonal),
                )
            }
            rows[offset + column - from] = current
            minimum = minOf(minimum, current)
        }
        return minimum
    }

    fun accepts(rows: IntArray, depth: Int, offset: Int): Boolean = query.length in start(depth) .. end(depth) && rows[offset + query.length - start(depth)] <= distance

    fun matches(text: String): Boolean {
        if (distance >= maxOf(query.length, text.length)) return true
        if (abs(query.length - text.length) > distance) return false
        if (text == query) return true
        val rows = scratch ?: IntArray(width * 2).also { scratch = it }
        for (column in 0 .. minOf(query.length, distance)) rows[column] = column
        var previous = 0
        var row = width
        for (index in text.indices) {
            if (advance(text[index], index + 1, rows, previous, row) > distance) return false
            val swap = previous
            previous = row
            row = swap
        }
        return accepts(rows, text.length, previous)
    }
}
