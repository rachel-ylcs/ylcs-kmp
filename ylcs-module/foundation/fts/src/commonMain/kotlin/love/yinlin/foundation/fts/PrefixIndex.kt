package love.yinlin.foundation.fts

/**
 * 每 32 个词项聚合一块，再构建区间树。宽前缀只合并 O(log(词典大小)) 个块和至多 62 个边界词项，
 * 不再为一次查询展开成千上万个词项。按需构建一次，读取期间没有可变缓存。
 */
internal class PrefixIndex(private val terms: Array<IndexedTerm>) {
    private val blockSize = 32
    private val base: Int
    private val nodes: Array<PostingList?>

    init {
        val blocks = (terms.size + blockSize - 1) / blockSize
        var power = 1
        while (power < blocks) power *= 2
        base = power
        nodes = arrayOfNulls(base * 2)
        val work = arrayOfNulls<PostingList>(blockSize)
        for (block in 0 ..< blocks) {
            var count = minOf(blockSize, terms.size - block * blockSize)
            for (index in 0 ..< count) work[index] = terms[block * blockSize + index].postings
            // 平衡合并，避免一个块反复复制逐渐增大的中间集合。
            while (count > 1) {
                var output = 0
                var input = 0
                while (input < count) {
                    work[output++] = if (input + 1 < count) PostingList.mergePostings(work[input]!!, work[input + 1]!!) else work[input]
                    input += 2
                }
                count = output
            }
            nodes[base + block] = work[0]
        }
        for (node in base - 1 downTo 1) {
            val left = nodes[node * 2]
            val right = nodes[node * 2 + 1]
            nodes[node] = if (left == null) right else if (right == null) left else PostingList.mergePostings(left, right)
        }
    }

    private fun MutableList<PostingList>.addPosting(posting: PostingList) {
        if (lastOrNull() !== posting) add(posting)
    }

    fun search(start: Int, end: Int, limit: Int): DocumentCursor {
        val firstBlock = (start + blockSize - 1) / blockSize
        val lastBlock = end / blockSize
        val lists = ArrayList<PostingList>(64)

        if (firstBlock >= lastBlock) {
            for (index in start ..< end) lists.addPosting(terms[index].postings)
        }
        else {
            for (index in start ..< firstBlock * blockSize) lists.addPosting(terms[index].postings)
            var left = firstBlock + base
            var right = lastBlock + base
            while (left < right) {
                if (left and 1 != 0) lists.addPosting(nodes[left++]!!)
                if (right and 1 != 0) lists.addPosting(nodes[--right]!!)
                left = left ushr 1
                right = right ushr 1
            }
            for (index in lastBlock * blockSize ..< end) lists.addPosting(terms[index].postings)
        }
        return DocumentCursor.union(lists, limit)
    }
}
