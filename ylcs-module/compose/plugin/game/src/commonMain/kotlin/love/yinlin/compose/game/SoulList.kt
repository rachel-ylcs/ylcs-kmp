package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.traits.Soul
import love.yinlin.compose.game.traits.Visible

@Stable
class SoulList(list: List<Soul>) {
    @PublishedApi
    @Stable
    internal data class SoulEntry(val soul: Soul, val addIndex: Long)

    companion object {
        internal val comparator = Comparator<SoulEntry> { entry1, entry2 ->
            if (entry1.soul is Visible) {
                if (entry2.soul is Visible) {
                    val result = entry1.soul.zIndex.compareTo(entry2.soul.zIndex)
                    if (result == 0) entry1.addIndex.compareTo(entry2.addIndex) else result
                }
                else -1
            }
            else {
                if (entry2.soul is Visible) 1
                else entry1.addIndex.compareTo(entry2.addIndex)
            }
        }
    }

    @PublishedApi
    internal val items = mutableListOf<SoulEntry>()

    private var soulAddIndex: Long = 0L

    init {
        for (item in list) {
            // push
            val entry = SoulEntry(item, soulAddIndex++)
            // 优先队列 -> 二分查找
            var index = items.binarySearch(entry, comparator)
            index = -index - 1
            items.add(index, entry)
        }
    }

    inline fun forEachForward(block: (Soul) -> Unit) {
        if (items.isEmpty()) return
        for (index in items.indices) block(items[index].soul)
    }

    inline fun forEachReverse(block: (Soul) -> Unit) {
        if (items.isEmpty()) return
        val reversed = items.asReversed()
        for (index in reversed.indices) block(reversed[index].soul)
    }
}