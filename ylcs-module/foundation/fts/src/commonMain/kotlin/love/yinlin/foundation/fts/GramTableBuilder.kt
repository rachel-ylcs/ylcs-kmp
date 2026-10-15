package love.yinlin.foundation.fts

internal class GramTableBuilder {
    private var high = IntArray(64)
    private var low = IntArray(64)
    private var values = arrayOfNulls<PostingBuilder>(64)
    private var size = 0

    private fun slot(first: Int, second: Int): Int {
        var index = GramTable.hash(first, second) and (high.size - 1)
        while (high[index] != 0 && (high[index] != first || low[index] != second)) index = (index + 1) and (high.size - 1)
        return index
    }

    private fun grow() {
        val previousHigh = high
        val previousLow = low
        val previousValues = values
        high = IntArray(high.size * 2)
        low = IntArray(high.size)
        values = arrayOfNulls(high.size)
        for (index in previousHigh.indices) {
            if (previousHigh[index] == 0) continue
            val next = slot(previousHigh[index], previousLow[index])
            high[next] = previousHigh[index]
            low[next] = previousLow[index]
            values[next] = previousValues[index]
        }
    }

    fun add(first: Int, second: Int, document: Int) {
        var index = slot(first, second)
        if (high[index] == 0) {
            if (size >= high.size - high.size / 4) {
                grow()
                index = slot(first, second)
            }
            high[index] = first
            low[index] = second
            values[index] = PostingBuilder()
            ++size
        }
        values[index]!!.add(document)
    }

    fun freeze(interner: PostingInterner): GramTable = GramTable(high, low, Array(values.size) {
        values[it]?.let { builder -> interner.intern(builder.build()) }
    })
}