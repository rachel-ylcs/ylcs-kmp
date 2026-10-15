package love.yinlin.foundation.fts

import kotlin.text.iterator

internal class NGramIndex(fields: IndexedFields<*>, interner: PostingInterner) {
    private val table: GramTable

    init {
        val builder = GramTableBuilder()
        for (document in 0 ..< fields.starts.size - 1) {
            for (field in fields.starts[document] ..< fields.starts[document + 1]) {
                var first = -1
                var second = -1
                for (char in fields.folded[field]) {
                    val current = char.code
                    builder.add(1, current, document)
                    if (second >= 0) builder.add(2, (second shl 16) or current, document)
                    if (first >= 0) builder.add((first shl 2) or 3, (second shl 16) or current, document)
                    first = second
                    second = current
                }
            }
        }
        table = builder.freeze(interner)
    }

    fun candidates(text: String): DocumentCursor {
        if (text.length == 1) return table.get(1, text[0].code)?.cursor() ?: EmptyCursor
        if (text.length == 2) return table.get(2, (text[0].code shl 16) or text[1].code)?.cursor() ?: EmptyCursor
        if (text.length == 3) return table.get((text[0].code shl 2) or 3, (text[1].code shl 16) or text[2].code)?.cursor() ?: EmptyCursor

        // 原字段还会核验，因此只需保留最多六个最有区分力的 gram，不为长查询构建 HashSet 或长排序表。
        val selected = ArrayList<PostingList>(6)
        var first = text[0].code
        var second = text[1].code
        for (index in 2 ..< text.length) {
            val current = text[index].code
            val posting = table.get((first shl 2) or 3, (second shl 16) or current) ?: return EmptyCursor
            if (posting.size == 1) return posting.cursor()
            var duplicate = false
            for (existing in selected) if (existing === posting) { duplicate = true; break }
            if (!duplicate) {
                if (selected.size < 6) selected.add(posting)
                else {
                    var largest = 0
                    for (candidate in 1 ..< selected.size) if (selected[candidate].size > selected[largest].size) largest = candidate
                    if (posting.size < selected[largest].size) selected[largest] = posting
                }
            }
            first = second
            second = current
        }
        return DocumentCursor.intersect(selected)
    }
}
