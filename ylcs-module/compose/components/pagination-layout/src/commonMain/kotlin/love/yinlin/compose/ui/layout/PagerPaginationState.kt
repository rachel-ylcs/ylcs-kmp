package love.yinlin.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * PagerPagination 的分页状态
 */
@Stable
class PagerPaginationState(initialPage: Int = 1) {
    var currentPage by mutableIntStateOf(initialPage.coerceAtLeast(1))
        private set

    var pageCount by mutableIntStateOf(0)
        private set

    fun goToPage(page: Int) {
        if (pageCount > 0) currentPage = page.coerceIn(1, pageCount)
    }

    fun previousPage() {
        goToPage(currentPage - 1)
    }

    fun nextPage() {
        if (currentPage < pageCount) currentPage++
    }

    internal fun updatePageCount(count: Int) {
        pageCount = count.coerceAtLeast(0)
        if (pageCount > 0) currentPage = currentPage.coerceIn(1, pageCount)
    }
}
