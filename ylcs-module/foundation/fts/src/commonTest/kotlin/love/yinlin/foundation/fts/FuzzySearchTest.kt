package love.yinlin.foundation.fts

import kotlin.test.Test
import kotlin.test.assertEquals

class FuzzySearchTest {
    @Test
    fun denseRowsAndBranchSnapshotsAgreeAcrossTheirWorkingSetBoundary() {
        val index = indexOf(["a".repeat(1363)], ["a".repeat(1364)], ["a".repeat(1365)])
        assertEquals([0, 1], index.search("a".repeat(1363), maxEditDistance = 1).ids())
        assertEquals([0, 1, 2], index.search("a".repeat(1364), maxEditDistance = 1).ids())
    }

    @Test
    fun branchingAfterLongSharedPathsRestoresTheCorrectParentRow() {
        val prefix = "a".repeat(12000)
        val words = [prefix + "x", prefix + "y", prefix + "yz", "b", "bb", "bc"]
        val index = FullTextSearch(words) { [it] }
        assertEquals([0, 1, 2], index.search(prefix + "z", maxEditDistance = 1).map { words.indexOf(it) })
        assertEquals(["b", "bb", "bc"], index.search("b", maxEditDistance = 1))
    }

    @Test
    fun supportsInsertionDeletionAndSubstitution() {
        val index = indexOf(["button"], ["slider"])
        for (query in ["buton", "buttton", "batton", "button"]) {
            assertEquals([0], index.search(query, maxEditDistance = 1).ids(), query)
        }
        assertEquals([0], index.search("buton", SearchMode.Fuzzy).ids())
        assertEquals([], index.search("buttonzzz", maxEditDistance = 1))
    }

    @Test
    fun transpositionCostsTwoEdits() {
        val index = indexOf(["button"])
        assertEquals([], index.search("butotn", maxEditDistance = 1))
        assertEquals([0], index.search("butotn", maxEditDistance = 2).ids())
    }

    @Test
    fun zeroDistanceUsesWholeWordSemantics() {
        val index = indexOf(["Button"], ["PrimaryButton"], ["button"])
        assertEquals([0, 2], index.search("button", maxEditDistance = 0).ids())
        assertEquals([2], index.search("button", maxEditDistance = 0, ignoreCase = false).ids())
        assertEquals([], index.search("but", maxEditDistance = 0))
    }

    @Test
    fun caseSensitiveFuzzySearchChecksRawVariants() {
        val index = indexOf(["button"], ["Button"], ["BUTTON"])
        assertEquals([0, 1, 2], index.search("button", maxEditDistance = 1).ids())
        assertEquals([0, 1], index.search("button", maxEditDistance = 1, ignoreCase = false).ids())
        assertEquals([2], index.search("BUTTON", maxEditDistance = 1, ignoreCase = false).ids())
    }

    @Test
    fun allFuzzyQueryTermsMustMatch() {
        val index = indexOf(["Button", "Slider"], ["Button"], ["Slider"])
        assertEquals([0], index.search("buton slidr", maxEditDistance = 1).ids())
        assertEquals([], index.search("buton missing", maxEditDistance = 1))
    }

    @Test
    fun largeDistancesDoNotOverflowAndDoNotIncludeDocumentsWithoutWords() {
        val index = indexOf(["abc"], ["Z"], [], ["!!!"])
        assertEquals([0, 1], index.search("x", maxEditDistance = Int.MAX_VALUE).ids())
        assertEquals([0, 1], index.search("x", maxEditDistance = Int.MAX_VALUE, ignoreCase = false).ids())
    }

    @Test
    fun longWordsUseAnExplicitStackAndNarrowDistanceRows() {
        val text = "a".repeat(12_000)
        val changed = text.take(6_000) + "b" + text.drop(6_001)
        val index = FullTextSearch([text, "small"]) { [it] }
        assertEquals([text], index.search(changed, maxEditDistance = 1))
        assertEquals([text], index.search(changed, maxEditDistance = 1, ignoreCase = false))
    }

    @Test
    fun shortWordsAndTrieSiblingsAreNotLostByPruning() {
        val index = FullTextSearch(["a", "ab", "abc", "ac", "b", "ba", "c", "cab", "zebra"]) { [it] }
        assertEquals(["a", "ab", "ac", "b", "ba", "c"], index.search("a", maxEditDistance = 1))
        assertEquals(["ab", "abc", "ac"], index.search("abc", maxEditDistance = 1))
    }
}
