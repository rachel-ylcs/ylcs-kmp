package love.yinlin.foundation.fts

/** 当前文档单调递增；advanceTo 返回不小于 target 的第一个文档。 */
internal interface DocumentCursor {
    val estimate: Int
    val current: Int
    fun next(): Int
    fun advanceTo(target: Int): Int

    companion object {
        internal const val END_DOCUMENT: Int = Int.MAX_VALUE

        internal fun intersect(postings: List<PostingList>): DocumentCursor {
            if (postings.isEmpty() || postings.any { it.size == 0 }) return EmptyCursor
            if (postings.size == 1) return postings[0].cursor()
            if (postings.all { it.bitmap }) return BitmapIntersectionCursor(postings)
            return IntersectionCursor(postings.map { it.cursor() })
        }

        internal fun intersectCursors(cursors: List<DocumentCursor>): DocumentCursor {
            if (cursors.isEmpty() || cursors.any { it.estimate == 0 }) return EmptyCursor
            if (cursors.size == 1) return cursors[0]
            if (cursors.all { it is PostingCursor && it.posting.bitmap }) {
                return BitmapIntersectionCursor(cursors.map { (it as PostingCursor).posting })
            }
            return IntersectionCursor(cursors)
        }

        internal fun union(postings: List<PostingList>, limit: Int = Int.MAX_VALUE): DocumentCursor {
            if (postings.isEmpty()) return EmptyCursor
            if (postings.size == 1) return postings[0].cursor()
            val last = postings.maxOf { it.last }
            if (last < 0) return EmptyCursor
            val words = (last ushr 5) + 1
            val frequency = postings.sumOf { it.size.toLong() }
            // 稠密、宽并集逐 word OR，比给每个文档做一次堆调整更便宜。
            if (limit > 32 && postings.size >= 4 && frequency >= words.toLong() * 2) {
                val bits = IntArray(words)
                for (posting in postings) {
                    if (posting.bitmap) {
                        for (index in posting.data.indices) bits[index] = bits[index] or posting.data[index]
                    }
                    else {
                        for (document in posting.data) bits[document ushr 5] = bits[document ushr 5] or (1 shl (document and 31))
                    }
                }
                val size = bits.sumOf { it.countOneBits() }
                return PostingList(bits, size, true, last).cursor()
            }
            return UnionCursor(postings.filter { it.size != 0 })
        }
    }
}