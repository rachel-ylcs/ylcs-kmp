package love.yinlin.foundation.fts

/** 字段按文档连续存放，避免每个文档两层 List 及查询时的字段迭代器。 */
internal class IndexedFields<T>(
    items: List<T>,
    extract: (T) -> Iterable<String>
) {
    val originals: Array<String>
    val folded: Array<String>
    val starts = IntArray(items.size + 1)
    val maxLength: Int
    val minNonEmptyLength: Int

    init {
        val texts: MutableList<String> = []
        var maximum = 0
        var minimum = Int.MAX_VALUE
        for (document in items.indices) {
            starts[document] = texts.size
            for (text in extract(items[document])) {
                texts.add(text)
                maximum = maxOf(maximum, text.length)
                if (text.isNotEmpty()) minimum = minOf(minimum, text.length)
            }
        }
        starts[items.size] = texts.size
        originals = texts.toTypedArray()
        folded = Array(originals.size) { SearchTokenizer.foldCase(originals[it]) }
        maxLength = maximum
        minNonEmptyLength = minimum
    }

    fun contains(document: Int, query: String, ignoreCase: Boolean): Boolean {
        val texts = if (ignoreCase) folded else originals
        for (field in starts[document] ..< starts[document + 1]) if (texts[field].contains(query)) return true
        return false
    }

    fun matchesRegex(document: Int, regex: Regex): Boolean {
        val start = starts[document]
        val end = starts[document + 1]
        if (start == end) return false
        if (regex.containsMatchIn(originals[start])) return true
        for (field in start + 1 ..< end) if (regex.containsMatchIn(originals[field])) return true
        return false
    }
}
