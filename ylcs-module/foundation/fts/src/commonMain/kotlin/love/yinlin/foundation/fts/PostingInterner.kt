package love.yinlin.foundation.fts

/** 仅在构建期间使用：相同的文档集合共享同一份不可变倒排表。 */
internal class PostingInterner {
    private class Entry(val posting: PostingList, val next: Entry?)
    private val heads = HashMap<Int, Entry>()

    fun intern(posting: PostingList): PostingList {
        val hash = posting.data.contentHashCode() * 31 + posting.size
        var entry = heads[hash]
        while (entry != null) {
            val other = entry.posting
            if (other.size == posting.size && other.bitmap == posting.bitmap && other.data.contentEquals(posting.data)) return other
            entry = entry.next
        }
        heads[hash] = Entry(posting, heads[hash])
        return posting
    }
}