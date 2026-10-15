package love.yinlin.foundation.fts

internal class ExactIndex(fields: IndexedFields<*>) {
    private val terms: Map<String, IndexedTerm>

    init {
        val builder = TermDictionaryBuilder(PostingInterner())
        for (document in 0 ..< fields.starts.size - 1) {
            for (field in fields.starts[document] ..< fields.starts[document + 1]) {
                builder.add(fields.originals[field], fields.folded[field], document)
            }
        }
        terms = builder.freeze()
    }

    fun search(original: String, folded: String, ignoreCase: Boolean): DocumentCursor {
        val term = terms[folded] ?: return EmptyCursor
        return (if (ignoreCase) term.postings else term.exact(original))?.cursor() ?: EmptyCursor
    }
}
