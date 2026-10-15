package love.yinlin.foundation.fts

/** 稀疏表保存文档编号，稠密表保存 32 位 bitmap。冻结后不会修改。 */
internal class PostingList(val data: IntArray, val size: Int, val bitmap: Boolean, val last: Int) {
    fun cursor(): DocumentCursor = if (size == 0) EmptyCursor else PostingCursor(this)

    companion object {
        /** 构建前缀区间索引使用，产生不可变并集。不会为稀疏的巨大文档编号区间分配 bitmap。 */
        internal fun mergePostings(left: PostingList, right: PostingList): PostingList {
            if (left === right || right.size == 0) return left
            if (left.size == 0) return right
            val last = maxOf(left.last, right.last)
            val words = (last ushr 5) + 1
            if (left.size.toLong() + right.size >= maxOf(9L, words.toLong() * 2)) {
                val bits = IntArray(words)
                for (posting in [left, right]) {
                    if (posting.bitmap) for (index in posting.data.indices) bits[index] = bits[index] or posting.data[index]
                    else {
                        for (document in posting.data) bits[document ushr 5] = bits[document ushr 5] or (1 shl (document and 31))
                    }
                }
                val count = bits.sumOf { it.countOneBits() }
                if (count == left.size) return left
                if (count == right.size) return right
                return PostingList(bits, count, true, last)
            }
            if (!left.bitmap && !right.bitmap) {
                val output = IntArray(left.size + right.size)
                var a = 0
                var b = 0
                var size = 0
                while (a < left.size || b < right.size) {
                    val first = if (a < left.size) left.data[a] else DocumentCursor.END_DOCUMENT
                    val second = if (b < right.size) right.data[b] else DocumentCursor.END_DOCUMENT
                    val document = minOf(first, second)
                    output[size++] = document
                    if (first == document) ++a
                    if (second == document) ++b
                }
                if (size == left.size) return left
                if (size == right.size) return right
                return PostingList(if (size == output.size) output else output.copyOf(size), size, false, last)
            }
            val builder = PostingBuilder()
            val first = left.cursor()
            val second = right.cursor()
            var a = first.next()
            var b = second.next()
            while (a != DocumentCursor.END_DOCUMENT || b != DocumentCursor.END_DOCUMENT) {
                val document = minOf(a, b)
                builder.add(document)
                if (a == document) a = first.next()
                if (b == document) b = second.next()
            }
            return builder.build()
        }
    }
}