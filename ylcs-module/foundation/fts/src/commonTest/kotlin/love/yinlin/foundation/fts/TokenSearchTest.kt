package love.yinlin.foundation.fts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.random.Random

class TokenSearchTest {
    @Test
    fun broadPrefixesAndBlockBoundariesAgreeWithLinearSearch() {
        val random = Random(1909)
        val words = List(650) { index ->
            "${((if (index % 2 == 0) 'a' else 'A').code + index % 5).toChar()}${index.toString().padStart(4, '0')}"
        }
        val documents = List(190) { id ->
            TestDocument(id, List(5) { words.random(random) })
        }
        val index = FullTextSearch(documents) { it.fields }
        val queries = ["a", "b", "c", "d", "e", "a0", "c01", "e064", "f", "a000", "b000", "d063", "e0649"]
        for (query in queries) {
            for (ignoreCase in [false, true]) for (limit in [1, 31, 32, 33, 64, Int.MAX_VALUE]) {
                assertEquals(
                    expected = documents.filter { it.fields.any { word -> word.startsWith(query, ignoreCase) } }.take(limit),
                    actual = index.search(query, SearchMode.Prefix, ignoreCase, limit), "$query / $ignoreCase / $limit")
            }
        }
    }

    @Test
    fun wholeWordsDoNotMatchSubstringsOrSplitCamelCase() {
        val index = indexOf(["Button"], ["PrimaryButton"], ["ButtonState"], ["Icon Button"])
        assertEquals([0, 3], index.search("button", SearchMode.WholeWord).ids())
        assertEquals([1], index.search("primarybutton", SearchMode.WholeWord).ids())
    }

    @Test
    fun allQueryWordsMustMatchButMayUseDifferentFieldsAndOrder() {
        val index = indexOf(["red", "Button"], ["red Slider"], ["Button red"], ["Button"])
        assertEquals([0, 2], index.search("Button red", SearchMode.WholeWord).ids())
        assertEquals([0, 2], index.search("red Button red", SearchMode.WholeWord).ids())
        assertEquals([0, 2], index.search("but re", SearchMode.Prefix).ids())
        assertEquals([], index.search("red missing", SearchMode.WholeWord))
    }

    @Test
    fun wholeWordCaseSensitivityUsesOriginalTerms() {
        val index = indexOf(["Button button"], ["BUTTON"], ["button"])
        assertEquals([0], index.search("Button", SearchMode.WholeWord, ignoreCase = false).ids())
        assertEquals([0, 2], index.search("button", SearchMode.WholeWord, ignoreCase = false).ids())
        assertEquals([0, 1, 2], index.search("button", SearchMode.WholeWord).ids())
    }

    @Test
    fun prefixesUseTermStartsAndRespectCase() {
        val index = indexOf(["PrimaryButton"], ["PrimaryText"], ["primary"], ["xPrimary"], ["Pri"])
        assertEquals([0, 1, 2, 4], index.search("pri", SearchMode.Prefix).ids())
        assertEquals([0, 1, 4], index.search("Pri", SearchMode.Prefix, ignoreCase = false).ids())
        assertEquals([], index.search("PrimaryButtonX", SearchMode.Prefix))
        assertEquals([], index.search("zzz", SearchMode.Prefix))
    }

    @Test
    fun tokenizerRecognizesNumbersUnderscoresAndCombiningMarks() {
        assertEquals(["foo_bar", "v12", "cafe\u0301", "中文"], SearchTokenizer.Default.tokenize("foo_bar / v12, cafe\u0301! 中文"))
        val index = indexOf(["foo_bar v12 cafe\u0301 中文"])
        assertEquals([0], index.search("cafe\u0301", SearchMode.WholeWord).ids())
        assertEquals([], index.search("cafe", SearchMode.WholeWord))
        assertEquals([], index.search("foo", SearchMode.WholeWord))
        assertEquals([0], index.search("中").ids())
        assertEquals([], index.search("中", SearchMode.WholeWord))
    }

    @Test
    fun punctuationOnlyQueriesCannotMatchEveryDocument() {
        val index = indexOf(["Button"], ["!!!"])
        for (mode in [SearchMode.WholeWord, SearchMode.Prefix, SearchMode.Fuzzy]) {
            assertEquals([], index.search("!!!", mode), mode.toString())
        }
        assertEquals([1], index.search("!!!").ids())
    }

    @Test
    fun customTokenizerIsUsedForBothDocumentsAndQueries() {
        val tokenizer = SearchTokenizer { it.split('|').map(String::trim) }
        val index = FullTextSearch(["C++|颜色|", "C#|日历"], tokenizer) { [it] }
        assertEquals(["C++|颜色|"], index.search("C++", SearchMode.WholeWord))
        assertEquals(["C++|颜色|"], index.search("颜色|C++", SearchMode.WholeWord))
        assertEquals(["C++|颜色|"], index.search("颜", SearchMode.Prefix))
        assertEquals(["C++|颜色|"], index.search("颜色", maxEditDistance = 0))
        assertEquals([], index.search("|", SearchMode.WholeWord))
        assertEquals(["C#|日历"], index.search("#"))
    }
}
