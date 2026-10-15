package love.yinlin.foundation.fts

/** 两个 Int 编码 gram，避免 HashMap<Long, ...> 的装箱和 JS 的 Long 运算。high 的低两位保存长度。 */
internal class GramTable(val high: IntArray, val low: IntArray, val values: Array<PostingList?>) {
    companion object {
        internal fun hash(high: Int, low: Int): Int {
            var value = low xor (high * -1640531527)
            value = (value xor (value ushr 16)) * -2048144789
            return value xor (value ushr 13)
        }
    }

    fun get(first: Int, second: Int): PostingList? {
        var slot = hash(first, second) and (high.size - 1)
        while (high[slot] != 0) {
            if (high[slot] == first && low[slot] == second) return values[slot]
            slot = (slot + 1) and (high.size - 1)
        }
        return null
    }
}