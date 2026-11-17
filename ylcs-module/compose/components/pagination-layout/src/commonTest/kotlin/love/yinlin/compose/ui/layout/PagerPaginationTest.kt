package love.yinlin.compose.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class PagerPaginationTest {
    @Test
    fun pageTokensShowEveryPageWhenTheyFit() {
        assertEquals([1, 2, 3, 4, 5, 6], calculatePagerPageTokens(currentPage = 3, pageCount = 6, maxCount = 8))
    }

    @Test
    fun pageTokensCountEllipsesWithinTheLimit() {
        assertEquals([1, 2, 3, 4, 5, 6, 7, 8, null, 11], calculatePagerPageTokens(currentPage = 5, pageCount = 11, maxCount = 10))
        assertEquals([1, null, 4, 5, 6, 7, 8, 9, 10, 11], calculatePagerPageTokens(currentPage = 10, pageCount = 11, maxCount = 10))
        assertEquals([1, null, 5, 6, 7, 8, null, 20], calculatePagerPageTokens(currentPage = 7, pageCount = 20, maxCount = 8))
    }

    @Test
    fun pageTokensKeepTheCurrentPageNearEitherEdge() {
        assertEquals([1, 2, 3, 4, 5, 6, null, 20], calculatePagerPageTokens(currentPage = 1, pageCount = 20, maxCount = 8))
        assertEquals([1, null, 15, 16, 17, 18, 19, 20], calculatePagerPageTokens(currentPage = 20, pageCount = 20, maxCount = 8))
    }

    @Test
    fun pageTokensUseTheMinimumBudgetAtTheEllipsisBoundaries() {
        assertEquals([1, 2, 3, null, 20], calculatePagerPageTokens(currentPage = 3, pageCount = 20, maxCount = 5))
        assertEquals([1, null, 4, null, 20], calculatePagerPageTokens(currentPage = 4, pageCount = 20, maxCount = 5))
        assertEquals([1, null, 17, null, 20], calculatePagerPageTokens(currentPage = 17, pageCount = 20, maxCount = 5))
        assertEquals([1, null, 18, 19, 20], calculatePagerPageTokens(currentPage = 18, pageCount = 20, maxCount = 5))
    }

    @Test
    fun stateClampsPageWhenPageCountChanges() {
        val state = PagerPaginationState(initialPage = 7)

        state.updatePageCount(4)
        assertEquals(4, state.currentPage)
        assertEquals(4, state.pageCount)

        state.previousPage()
        assertEquals(3, state.currentPage)

        state.nextPage()
        assertEquals(4, state.currentPage)

        state.goToPage(99)
        assertEquals(4, state.currentPage)

        state.updatePageCount(0)
        assertEquals(0, state.pageCount)
        assertEquals(4, state.currentPage)
    }

    @Test
    fun nextPageStaysOnTheLastPageAtTheIntegerLimit() {
        val state = PagerPaginationState(initialPage = Int.MAX_VALUE)
        state.updatePageCount(Int.MAX_VALUE)

        state.nextPage()

        assertEquals(Int.MAX_VALUE, state.currentPage)
    }
}
