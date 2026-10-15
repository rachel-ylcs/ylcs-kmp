package love.yinlin.foundation.fts

internal class TermDictionaryBuilder(private val interner: PostingInterner) {
    var maxLength: Int = 0
        private set
    private val documents = PostingBuilder()
    private class Builder(val original: String) {
        val all = PostingBuilder()
        var variants: HashMap<String, PostingBuilder>? = null

        fun add(word: String, document: Int) {
            var forms = variants
            if (forms == null && word != original) {
                forms = hashMapOf(original to all.duplicate())
                variants = forms
            }
            forms?.getOrPut(word) { PostingBuilder() }?.add(document)
            all.add(document)
        }
    }

    private val builders = HashMap<String, Builder>()

    fun add(original: String, folded: String, document: Int) {
        if (original.isNotEmpty()) {
            maxLength = maxOf(maxLength, original.length)
            documents.add(document)
            builders.getOrPut(folded) { Builder(original) }.add(original, document)
        }
    }

    fun allDocuments(): PostingList = interner.intern(documents.build())

    fun freeze(): Map<String, IndexedTerm> = builders.mapValues { [text, builder] ->
        IndexedTerm(
            text = text,
            original = builder.original,
            postings = interner.intern(builder.all.build()),
            variants = builder.variants?.mapValues { interner.intern(it.value.build()) }
        )
    }
}