package love.yinlin.foundation.fts

/** 稀疏多路并集只推进当前最小文档，不先计算完整结果。 */
internal class UnionCursor(postings: List<PostingList>) : DocumentCursor {
    private val heap = postings.map { it.cursor().also { cursor -> cursor.next() } }.toTypedArray()
    private var count = heap.size
    override val estimate: Int = minOf(postings.sumOf { it.size.toLong() }, postings.maxOf { it.last }.toLong() + 1).toInt()
    override var current: Int = -1
        private set

    init {
        for (index in count / 2 - 1 downTo 0) siftDown(index)
    }

    private fun siftDown(start: Int) {
        var parent = start
        val cursor = heap[parent]
        var child = parent * 2 + 1
        while (child < count) {
            if (child + 1 < count && heap[child + 1].current < heap[child].current) ++child
            if (cursor.current <= heap[child].current) break
            heap[parent] = heap[child]
            parent = child
            child = parent * 2 + 1
        }
        heap[parent] = cursor
    }

    private fun update(target: Int) {
        if (heap[0].advanceTo(target) == DocumentCursor.END_DOCUMENT) heap[0] = heap[--count]
        if (count != 0) siftDown(0)
    }

    override fun next(): Int = if (current == DocumentCursor.END_DOCUMENT) current else advanceTo(current + 1)

    override fun advanceTo(target: Int): Int {
        if (current >= target) return current
        while (count != 0 && heap[0].current < target) update(target)
        current = if (count == 0) DocumentCursor.END_DOCUMENT else heap[0].current
        return current
    }
}