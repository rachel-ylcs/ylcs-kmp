package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import love.yinlin.extension.replaceAll

// 分页数据实现
@Stable
abstract class Pagination<E, K, out T>(
    val default: T,
    val pageNum: Int
) {
    val items = mutableStateListOf<E>()
    var canLoading by mutableStateOf(false)

    abstract fun distinctValue(item: E): K
    abstract fun offset(item: E): T
    private var mOffset: T = default
    val offset: T get() = mOffset

    open fun processArgs(last: E?) {}

    fun newData(newItems: List<E>): Boolean {
        items.replaceAll(newItems)
        val last = newItems.lastOrNull()
        mOffset = last?.let { offset(it) } ?: default
        processArgs(last)
        canLoading = newItems.size == pageNum
        return newItems.isNotEmpty()
    }

    fun moreData(newItems: List<E>): Boolean {
        if (newItems.isEmpty()) {
            mOffset = default
            processArgs(null)
        }
        else {
            val existingItems = items.fastMap { distinctValue(it) }.toSet()
            items += newItems.fastFilter { distinctValue(it) !in existingItems }
            val last = newItems.lastOrNull()
            mOffset = last?.let { offset(it) } ?: default
            processArgs(last)
        }
        canLoading = offset != default && newItems.size == pageNum
        return newItems.isNotEmpty()
    }
}