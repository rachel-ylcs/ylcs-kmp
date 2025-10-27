package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable

@Stable
abstract class PaginationArgs<E, K, out T, out A1>(
    default: T,
    private val default1: A1,
    pageNum: Int
) : Pagination<E, K, T>(default, pageNum) {
    abstract fun arg1(item: E): A1
    private var mArg1: A1 = default1
    val arg1: A1 get() = mArg1

    override fun processArgs(last: E?) {
        mArg1 = last?.let { arg1(it) } ?: default1
    }
}