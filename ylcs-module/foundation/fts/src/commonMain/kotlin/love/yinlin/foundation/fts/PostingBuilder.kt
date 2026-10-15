package love.yinlin.foundation.fts

internal class PostingBuilder {
    private var values = IntArray(4)
    private var size = 0

    fun add(value: Int) {
        if (size != 0 && values[size - 1] == value) return
        if (size == values.size) values = values.copyOf(values.size * 2)
        values[size++] = value
    }

    fun duplicate(): PostingBuilder = PostingBuilder().also {
        it.values = values.copyOf()
        it.size = size
    }

    fun build(): PostingList {
        if (size == 0) return PostingList([], 0, false, -1)
        val last = values[size - 1]
        val words = (last ushr 5) + 1
        if (size > 8 && size.toLong() >= words.toLong() * 2) {
            val bits = IntArray(words)
            for (index in 0 ..< size) {
                val document = values[index]
                bits[document ushr 5] = bits[document ushr 5] or (1 shl (document and 31))
            }
            return PostingList(bits, size, true, last)
        }
        return PostingList(values.copyOf(size), size, false, last)
    }
}