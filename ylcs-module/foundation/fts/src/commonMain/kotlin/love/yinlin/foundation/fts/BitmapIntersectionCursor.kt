package love.yinlin.foundation.fts

/** 所有输入都是 bitmap 时，按 word 求交，查询只保留一个 word 的状态。 */
internal class BitmapIntersectionCursor(private val lists: List<PostingList>) : DocumentCursor {
    private val end = lists.minOf { it.data.size }
    override val estimate: Int = lists.minOf { it.size }
    override var current: Int = -1
        private set
    private var word = -1
    private var remaining = 0

    override fun next(): Int {
        if (current == DocumentCursor.END_DOCUMENT) return current
        while (remaining == 0) {
            if (++word >= end) {
                current = DocumentCursor.END_DOCUMENT
                return current
            }
            var bits = -1
            for (list in lists) bits = bits and list.data[word]
            remaining = bits
        }
        current = (word shl 5) + remaining.countTrailingZeroBits()
        remaining = remaining and (remaining - 1)
        return current
    }

    override fun advanceTo(target: Int): Int {
        if (current >= target) return current
        word = target ushr 5
        if (word >= end) {
            current = DocumentCursor.END_DOCUMENT
            return current
        }
        var bits = -1 shl (target and 31)
        for (list in lists) bits = bits and list.data[word]
        remaining = bits
        if (bits != 0) {
            current = (word shl 5) + bits.countTrailingZeroBits()
            remaining = bits and (bits - 1)
            return current
        }
        return next()
    }
}