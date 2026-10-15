package love.yinlin.foundation.fts

import kotlin.collections.iterator

internal class TermIndex(
    fields: IndexedFields<*>,
    private val tokenizer: SearchTokenizer,
    interner: PostingInterner
) {
    private val terms: Map<String, IndexedTerm>
    private val maxWordLength: Int
    private val allDocuments: PostingList
    private val ordered by lazy { terms.values.sortedBy { it.text }.toTypedArray() }
    private val trie by lazy { WordTrie(ordered.map { it.text }) }
    private val prefixes by lazy { PrefixIndex(ordered) }
    private val rawOrdered by lazy {
        buildList {
            for (term in terms.values) {
                if (term.variants == null) add(if (term.text == term.original) term else IndexedTerm(term.original, term.original, term.postings, null))
                else {
                    for ([word, postings] in term.variants) {
                        add(IndexedTerm(word, word, postings, null))
                    }
                }
            }
        }.sortedBy { it.text }.toTypedArray()
    }
    private val rawPrefixes by lazy { PrefixIndex(rawOrdered) }

    init {
        val builder = TermDictionaryBuilder(interner)
        for (document in 0 ..< fields.starts.size - 1) {
            for (field in fields.starts[document] ..< fields.starts[document + 1]) {
                val text = fields.originals[field]
                if (tokenizer === SearchTokenizer.Default) {
                    SearchTokenizer.scanDefaultWords(text) { word ->
                        builder.add(word, SearchTokenizer.foldCase(word), document)
                        true
                    }
                }
                else {
                    for (word in tokenizer.tokenize(text)) {
                        builder.add(word, SearchTokenizer.foldCase(word), document)
                    }
                }
            }
        }
        terms = builder.freeze()
        maxWordLength = builder.maxLength
        allDocuments = builder.allDocuments()
    }

    fun prepare(mode: SearchMode, ignoreCase: Boolean = true) {
        when (mode) {
            SearchMode.Prefix -> if (ignoreCase) prefixes else rawPrefixes
            SearchMode.Fuzzy -> trie
            else -> Unit
        }
    }

    fun search(text: String, mode: SearchMode, ignoreCase: Boolean, distance: Int, limit: Int): DocumentCursor {
        val cursors: MutableList<DocumentCursor> = []
        fun accept(word: String): Boolean {
            if (word.isEmpty()) return true
            if (word.length > maxWordLength && (mode != SearchMode.Fuzzy || word.length - maxWordLength > distance)) return false
            val folded = if (mode == SearchMode.Prefix && !ignoreCase) word else SearchTokenizer.foldCase(word)
            val cursor = when {
                mode == SearchMode.WholeWord || (mode == SearchMode.Fuzzy && distance == 0) -> {
                    val term = terms[folded]
                    (if (ignoreCase) term?.postings else term?.exact(word))?.cursor() ?: EmptyCursor
                }
                mode == SearchMode.Prefix -> prefix(word, folded, ignoreCase, limit)
                else -> fuzzy(word, folded, ignoreCase, distance, limit)
            }
            if (cursor.estimate == 0) return false
            // 相同文档集合已在构建时合并，重复查询词及等价倒排表无需重复求交。
            if (cursor is PostingCursor) {
                for (existing in cursors) {
                    if (existing is PostingCursor && existing.posting === cursor.posting) return true
                }
            }
            cursors.add(cursor)
            return true
        }
        if (tokenizer === SearchTokenizer.Default) {
            if (!SearchTokenizer.scanDefaultWords(text, ::accept)) return EmptyCursor
        }
        else {
            for (word in tokenizer.tokenize(text)) {
                if (!accept(word)) return EmptyCursor
            }
        }
        return DocumentCursor.intersectCursors(cursors)
    }

    private fun prefix(word: String, folded: String, ignoreCase: Boolean, limit: Int): DocumentCursor {
        val vocabulary = if (ignoreCase) ordered else rawOrdered
        val query = if (ignoreCase) folded else word
        var low = 0
        var high = vocabulary.size
        while (low < high) {
            val middle = low + (high - low) / 2
            if (vocabulary[middle].text < query) low = middle + 1 else high = middle
        }
        val start = low
        high = vocabulary.size
        // [start, end) 是连续前缀区间，不必对其中每个词项重复检查折叠前缀。
        while (low < high) {
            val middle = low + (high - low) / 2
            if (vocabulary[middle].text.startsWith(query)) low = middle + 1 else high = middle
        }
        if (low - start > 64) return (if (ignoreCase) prefixes else rawPrefixes).search(start, low, limit)
        val lists = ArrayList<PostingList>(minOf(low - start, 64))
        for (index in start ..< low) {
            addPosting(lists, vocabulary[index].postings)
        }
        return DocumentCursor.union(lists, limit)
    }

    private fun fuzzy(word: String, folded: String, ignoreCase: Boolean, distance: Int, limit: Int): DocumentCursor {
        if (distance >= maxOf(word.length, maxWordLength)) return allDocuments.cursor()
        val lists: MutableList<PostingList> = []
        val matcher = if (ignoreCase) null else EditDistance(word, distance)
        trie.search(folded, distance) { index ->
            val term = ordered[index]
            if (ignoreCase) addPosting(lists, term.postings)
            else if (term.variants == null) {
                if ((word == folded && term.original == term.text) || matcher!!.matches(term.original)) addPosting(lists, term.postings)
            }
            else {
                for ([variant, postings] in term.variants) {
                    if ((word == folded && variant == term.text) || matcher!!.matches(variant)) addPosting(lists, postings)
                }
            }
        }
        return DocumentCursor.union(lists, limit)
    }

    private fun addPosting(lists: MutableList<PostingList>, posting: PostingList) {
        if (lists.lastOrNull() !== posting) lists.add(posting)
    }
}
