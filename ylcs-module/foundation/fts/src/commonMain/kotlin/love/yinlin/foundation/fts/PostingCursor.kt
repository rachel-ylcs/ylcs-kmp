package love.yinlin.foundation.fts

internal class PostingCursor(val posting: PostingList) : DocumentCursor {
    override val estimate: Int get() = posting.size
    override var current: Int = -1
        private set
    private var position = -1
    private var remaining = 0

    override fun next(): Int {
        if (current == DocumentCursor.END_DOCUMENT) return current
        if (!posting.bitmap) {
            current = if (++position < posting.data.size) posting.data[position] else DocumentCursor.END_DOCUMENT
        }
        else {
            while (remaining == 0) {
                if (++position == posting.data.size) {
                    current = DocumentCursor.END_DOCUMENT
                    return current
                }
                remaining = posting.data[position]
            }
            current = (position shl 5) + remaining.countTrailingZeroBits()
            remaining = remaining and (remaining - 1)
        }
        return current
    }

    override fun advanceTo(target: Int): Int {
        if (current >= target) return current
        if (target > posting.last) {
            current = DocumentCursor.END_DOCUMENT
            return current
        }
        if (posting.bitmap) {
            position = target ushr 5
            remaining = posting.data[position] and (-1 shl (target and 31))
            if (remaining != 0) {
                current = (position shl 5) + remaining.countTrailingZeroBits()
                remaining = remaining and (remaining - 1)
                return current
            }
            // next() 从下一 word 开始，不能重新读取已经排除的目标 word。
            return next()
        }
        val data = posting.data
        var low = position + 1
        var high = low
        var step = 1
        // 指数跳跃先缩小区间，避免每个候选都二分整个剩余长表。
        while (high < data.size && data[high] < target) {
            low = high + 1
            high = if (step >= data.size - high) data.size else high + step
            step = if (step <= Int.MAX_VALUE / 2) step * 2 else Int.MAX_VALUE
        }
        high = minOf(data.size, high + if (high < data.size) 1 else 0)
        while (low < high) {
            val middle = low + (high - low) / 2
            if (data[middle] < target) low = middle + 1 else high = middle
        }
        position = low
        current = if (low < data.size) data[low] else DocumentCursor.END_DOCUMENT
        return current
    }
}