package love.yinlin.foundation.fts

/**
 * 多字段、内存全文检索索引
 *
 * [fields] 在构建时对每个 item 调用一次并立即复制。索引保存文本快照和原 item 引用；
 * item 后续变更不会自动更新索引，应重新构建。每个输入位置是一个文档；一个文档命中多次
 * 也只返回一次，结果始终按输入顺序排列。[SearchOptions.limit] 在此顺序上截取。
 *
 * 空字符串查询返回全部文档；其他查询保留空格和标点，不隐式 trim。
 * 词项模式中，非空查询如果分不出词项则返回空列表。
 *
 * 编辑距离与索引位置以 UTF-16 Char 为单位；不做 Unicode 规范化、词干化或相关度排序。
 */
class FullTextSearch<T>(
    items: Iterable<T>,
    tokenizer: SearchTokenizer = SearchTokenizer.Default,
    fields: (T) -> Iterable<String>,
) {
    private val items = items.toList()
    private val fields = IndexedFields(this.items, fields)
    private val interner = PostingInterner()
    private val grams = NGramIndex(this.fields, interner)
    private val terms = TermIndex(this.fields, tokenizer, interner)
    private val exact by lazy { ExactIndex(this.fields) }

    val size: Int get() = items.size

    /**
     * 提前构建指定模式的延迟索引，把首次 Exact/宽 Prefix/Fuzzy 的初始化成本放在查询之前。
     * 无模式参数时准备全部模式；ignoreCase 选择前缀词典的大小写方向。
     * 可以在发布索引给查询线程前调用。返回当前实例以便链式使用。
     */
    fun prepare(vararg modes: SearchMode, ignoreCase: Boolean = true): FullTextSearch<T> {
        if (modes.isEmpty()) {
            exact
            terms.prepare(SearchMode.Prefix, ignoreCase)
            terms.prepare(SearchMode.Fuzzy)
        }
        else {
            for (mode in modes) {
                if (mode == SearchMode.Exact) exact else terms.prepare(mode, ignoreCase)
            }
        }
        return this
    }

    // 字面子串搜索，默认忽略大小写
    fun search(text: String, ignoreCase: Boolean = true, limit: Int = Int.MAX_VALUE): List<T> = search(text, SearchMode.Contains, ignoreCase, 1, limit)

    // 按指定模式搜索；Fuzzy 默认允许一次编辑
    fun search(text: String, mode: SearchMode, ignoreCase: Boolean = true, limit: Int = Int.MAX_VALUE): List<T> = search(text, mode, ignoreCase, 1, limit)

    // 模糊搜索，允许指定次数的插入、删除和替换；交换相邻字符算两次编辑
    fun search(text: String, maxEditDistance: Int, ignoreCase: Boolean = true, limit: Int = Int.MAX_VALUE): List<T> = search(text, SearchMode.Fuzzy, ignoreCase, maxEditDistance, limit)

    fun search(text: String, options: SearchOptions): List<T> = search(text, options.mode, options.ignoreCase, options.maxEditDistance, options.limit)

    private fun search(text: String, mode: SearchMode, ignoreCase: Boolean, distance: Int, limit: Int): List<T> {
        require(limit >= 0) { "limit must be non-negative" }
        require(distance >= 0) { "maxEditDistance must be non-negative" }
        if (limit == 0 || items.isEmpty()) return []
        if (text.isEmpty()) return items.take(limit)
        return when (mode) {
            SearchMode.Exact -> {
                if (text.length < fields.minNonEmptyLength || text.length > fields.maxLength) []
                else collect(exact.search(text, SearchTokenizer.foldCase(text), ignoreCase), limit)
            }
            SearchMode.Contains -> {
                if (text.length > fields.maxLength) return []
                val folded = SearchTokenizer.foldCase(text)
                val candidates = grams.candidates(folded)
                // 单个 gram 已证明存在于同一字段，忽略大小写的短查询无需再扫描字段。
                if (ignoreCase && text.length <= 3) collect(candidates, limit)
                else {
                    collectMatching(candidates, limit) { document ->
                        fields.contains(document, if (ignoreCase) folded else text, ignoreCase)
                    }
                }
            }
            else -> collect(terms.search(text, mode, ignoreCase, distance, limit), limit)
        }
    }

    // 每个字段单独执行 containsMatchIn；Regex 的编译及大小写选项由调用者控制
    fun search(regex: Regex, limit: Int = Int.MAX_VALUE): List<T> {
        require(limit >= 0) { "limit must be non-negative" }
        if (limit == 0) return []
        val result: MutableList<T> = []
        for (document in items.indices) {
            if (fields.matchesRegex(document, regex)) {
                result.add(items[document])
                if (result.size == limit) break
            }
        }
        return result
    }

    private fun collect(documents: DocumentCursor, limit: Int): List<T> {
        // 精确倒排集合覆盖全部输入位置时，标准库可直接批量复制列表，无需逐个解码文档编号。
        if (documents is PostingCursor && documents.current < 0 && documents.posting.size == items.size) return items.take(limit)
        return collectMatching(documents, limit) { true }
    }

    private inline fun collectMatching(documents: DocumentCursor, limit: Int, matches: (Int) -> Boolean): List<T> {
        if (documents.estimate == 0) return []
        val result = ArrayList<T>(minOf(documents.estimate, limit))
        // 单表是最常见路径：直接枚举局部数组/位图，不对每个命中维护游标对象状态。
        if (documents is PostingCursor && documents.current < 0) {
            val posting = documents.posting
            if (posting.bitmap) {
                for (word in posting.data.indices) {
                    var bits = posting.data[word]
                    while (bits != 0) {
                        val document = (word shl 5) + bits.countTrailingZeroBits()
                        bits = bits and (bits - 1)
                        if (matches(document)) {
                            result.add(items[document])
                            if (result.size == limit) return result
                        }
                    }
                }
            }
            else {
                for (document in posting.data) {
                    if (matches(document)) {
                        result.add(items[document])
                        if (result.size == limit) return result
                    }
                }
            }
            return result
        }
        var document = documents.next()
        while (document != DocumentCursor.END_DOCUMENT) {
            if (matches(document)) {
                result.add(items[document])
                if (result.size == limit) break
            }
            document = documents.next()
        }
        return result
    }
}
