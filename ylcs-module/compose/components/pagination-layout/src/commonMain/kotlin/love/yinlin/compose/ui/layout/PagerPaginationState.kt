package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember

/**
 * PagerPagination 的分页状态。
 *
 * [currentPage] 从 1 开始。没有数据时 [pageCount] 为 0，当前页保留为至少 1；
 * 数据变化导致总页数减少时，当前页会自动调整到有效范围。
 */
@Stable
class PagerPaginationState(initialPage: Int = 1) {
    /** 当前页码，从 1 开始。 */
    var currentPage by mutableIntStateOf(initialPage.coerceAtLeast(1))
        private set

    /** 当前数据对应的总页数；没有数据时为 0。 */
    var pageCount by mutableIntStateOf(0)
        private set

    /** 切换到指定页，超出范围时自动限制在有效页码内。 */
    fun goToPage(page: Int) {
        if (pageCount > 0) currentPage = page.coerceIn(1, pageCount)
    }

    /** 切换到上一页。 */
    fun previousPage() {
        goToPage(currentPage - 1)
    }

    /** 切换到下一页。 */
    fun nextPage() {
        goToPage(currentPage + 1)
    }

    internal fun updatePageCount(count: Int) {
        pageCount = count.coerceAtLeast(0)
        if (pageCount > 0) currentPage = currentPage.coerceIn(1, pageCount)
    }
}

/** 创建并记住 PagerPagination 的分页状态。 */
@Composable
fun rememberPagerPaginationState(initialPage: Int = 1): PagerPaginationState =
    remember { PagerPaginationState(initialPage) }
