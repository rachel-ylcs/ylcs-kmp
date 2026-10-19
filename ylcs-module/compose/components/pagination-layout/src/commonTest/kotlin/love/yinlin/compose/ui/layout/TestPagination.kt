package love.yinlin.compose.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPagination {
    private class Pages : Pagination<Int, Int, Int>(default = 0, pageNum = 3) {
        var lastProcessed: Int? = null
        override fun distinctValue(item: Int): Int = item
        override fun offset(item: Int): Int = item
        override fun processArgs(last: Int?) { lastProcessed = last }
    }

    @Test
    fun overlappingPagesKeepOrderAndUseLastRawItemAsCursor() {
        val pages = Pages()
        assertTrue(pages.newData([1, 2, 3]))
        assertTrue(pages.canLoading)
        assertTrue(pages.moreData([2, 3, 4]))
        assertEquals([1, 2, 3, 4], pages.items.toList())
        assertEquals(4, pages.offset)
        assertEquals(4, pages.lastProcessed)
        assertTrue(pages.canLoading)
    }

    @Test
    fun emptyPageStopsLoadingAndResetsCursorWithoutRemovingItems() {
        val pages = Pages()
        pages.newData([1, 2, 3])
        assertFalse(pages.moreData([]))
        assertEquals([1, 2, 3], pages.items.toList())
        assertEquals(0, pages.offset)
        assertEquals(null, pages.lastProcessed)
        assertFalse(pages.canLoading)
    }

    @Test
    fun mutableItemsRemainSourceOfTruthForDeduplication() {
        val pages = Pages()
        pages.newData([1, 2, 3])
        pages.items.remove(2)
        pages.moreData([2, 3, 4])
        assertEquals([1, 3, 2, 4], pages.items.toList())
    }

    @Test
    fun duplicatesWithinIncomingPageKeepExistingContract() {
        val pages = Pages()
        pages.newData([1])
        pages.moreData([1, 2, 2])
        assertEquals([1, 2, 2], pages.items.toList())
        pages.newData([])
        assertTrue(pages.items.isEmpty())
        assertFalse(pages.canLoading)
    }
}