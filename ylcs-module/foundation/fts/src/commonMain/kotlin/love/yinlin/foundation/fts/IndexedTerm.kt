package love.yinlin.foundation.fts

/** 单一词形直接复用总倒排表；只有出现第二种原始大小写时才建立变体字典。 */
internal class IndexedTerm(
    val text: String,
    val original: String,
    val postings: PostingList,
    val variants: Map<String, PostingList>?,
) {
    fun exact(word: String): PostingList? = if (variants == null) {
        if (original == word) postings else null
    }
    else variants[word]
}