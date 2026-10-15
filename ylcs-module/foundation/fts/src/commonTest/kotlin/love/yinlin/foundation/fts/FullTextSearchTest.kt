package love.yinlin.foundation.fts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

internal data class TestDocument(val id: Int, val fields: List<String>)

internal fun indexOf(vararg fields: List<String>): FullTextSearch<TestDocument> =
    FullTextSearch(fields.mapIndexed { id, texts -> TestDocument(id, texts) }) { it.fields }

internal fun List<TestDocument>.ids(): List<Int> = map { it.id }

class FullTextSearchTest {
    @Test
    fun lengthPruningKeepsCustomTokensIndependentOfOriginalFieldLengths() {
        val alias = "a_very_long_keyword"
        val tokenizer = SearchTokenizer { if (it == "x") [alias] else [it] }
        val index = FullTextSearch(["x"], tokenizer) { [it] }
        assertEquals([], index.search(alias))
        assertEquals([], index.search(alias, SearchMode.Exact))
        assertEquals(["x"], index.search(alias, SearchMode.WholeWord))
        assertEquals(["x"], index.search("a_very", SearchMode.Prefix))
        assertEquals(["x"], index.search(alias, maxEditDistance = 0))
        assertEquals([], index.search(alias + "___", maxEditDistance = 1))
        assertEquals([], index.search(alias + "___", SearchMode.Prefix))
    }

    @Test
    fun preparedIndexesPreserveAllModesAndDoNotRetainMutableQueryState() {
        val index = indexOf(["Button input"], ["button", "Slider"], [])
        assertSame(index, index.prepare())
        assertSame(index, index.prepare(SearchMode.Exact, SearchMode.Prefix, SearchMode.Fuzzy))
        assertSame(index, index.prepare(SearchMode.Prefix, ignoreCase = false))
        repeat(3) {
            assertEquals([1], index.search("BUTTON", SearchMode.Exact).ids())
            assertEquals([0, 1], index.search("but", SearchMode.Prefix).ids())
            assertEquals([0, 1], index.search("buton", maxEditDistance = 1).ids())
            assertEquals([], index.search("BUTTON", SearchMode.Exact, ignoreCase = false))
        }
    }
    @Test
    fun literalSubstringSearchesEveryFieldAndReturnsEachDocumentOnce() {
        val index = indexOf(["Input", "PasswordInput InputState"], ["Button", "IconButton"], ["Other"])
        assertEquals([0], index.search("input").ids())
        assertEquals([0], index.search("InputState").ids())
        assertEquals([1], index.search("Button").ids())
        assertEquals([], index.search("missing"))
    }

    @Test
    fun caseSensitiveAndInsensitiveOverloads() {
        val index = indexOf(["Button"], ["button"], ["BUTTON"])
        assertEquals([0, 1, 2], index.search("button").ids())
        assertEquals([1], index.search("button", ignoreCase = false).ids())
        assertEquals([2], index.search("BUTTON", SearchOptions(ignoreCase = false)).ids())
    }

    @Test
    fun oneAndTwoCharacterQueriesIncludeChineseAndPunctuation() {
        val index = indexOf(["颜色选择器"], ["日历"], ["C++ / []"])
        assertEquals([0], index.search("色").ids())
        assertEquals([0], index.search("选择").ids())
        assertEquals([1], index.search("日历").ids())
        assertEquals([2], index.search("+").ids())
        assertEquals([2], index.search("[]").ids())
    }

    @Test
    fun literalSearchNeverInterpretsRegexMetacharacters() {
        val index = indexOf(["a.*b"], ["axyzb"], ["[Button]"])
        assertEquals([0], index.search("a.*b").ids())
        assertEquals([2], index.search("[Button]").ids())
    }

    @Test
    fun gramsAreVerifiedAgainstContiguousTextInOneField() {
        val index = indexOf(["abc", "bcd"], ["abc-bcd"], ["abcd"], ["cdab bc"])
        assertEquals([2], index.search("abcd").ids())
        assertEquals([2], index.search("abcd", limit = 1).ids())
    }

    @Test
    fun repeatedAndOverlappingGramsDoNotCreateFalseMatches() {
        val index = indexOf(["aaaa"], ["aaaaa"], ["abababa"], ["abab"])
        assertEquals([1], index.search("aaaaa").ids())
        assertEquals([2], index.search("ababa").ids())
    }

    @Test
    fun exactModeMatchesTheEntireField() {
        val index = indexOf(["Button"], ["PrimaryButton"], [" Button "], ["button"])
        assertEquals([0, 3], index.search("button", SearchMode.Exact).ids())
        assertEquals([0], index.search("Button", SearchMode.Exact, ignoreCase = false).ids())
        assertEquals([2], index.search(" Button ", SearchMode.Exact).ids())
    }

    @Test
    fun emptyQueriesAndEmptyIndexes() {
        val index = indexOf([], [""], ["Button"])
        for (mode in SearchMode.entries) assertEquals([0, 1, 2], index.search("", mode).ids())
        assertEquals([0, 1], index.search("", limit = 2).ids())
        assertEquals([], indexOf().search("anything"))
        assertEquals([], indexOf().search(Regex(".*")))
    }

    @Test
    fun whitespaceIsLiteralAndIsNotImplicitlyTrimmed() {
        val index = indexOf(["a b"], ["ab"], [" "])
        assertEquals([0, 2], index.search(" ").ids())
        assertEquals([], index.search("  "))
        assertEquals([], index.search(" ", SearchMode.WholeWord))
    }

    @Test
    fun regexUsesOriginalIndependentFieldsAndItsOwnOptions() {
        val index = indexOf(["Button"], ["button"], ["Primary", "Button"], ["PrimaryButton"])
        assertEquals([0, 2], index.search(Regex("^Button$")).ids())
        assertEquals([0, 1, 2], index.search(Regex("^button$", RegexOption.IGNORE_CASE)).ids())
        assertEquals([3], index.search(Regex("Primary.*Button")).ids())
        assertEquals([0], index.search(Regex("Button"), limit = 1).ids())
    }

    @Test
    fun regexCanMatchAnEmptyFieldButNotAnAbsentField() {
        val index = indexOf([], [""], ["a"])
        assertEquals([1], index.search(Regex("^$")).ids())
        assertEquals([1, 2], index.search(Regex("")).ids())
    }

    @Test
    fun unicodeCaseFoldingAgreesWithKotlinIgnoreCase() {
        val fields = ["ΟΣ", "οσ", "ος", "I", "i", "İ", "ı", "K", "k", "ſ", "S", "ß", "SS"]
        val index = FullTextSearch(fields) { [it] }
        for (query in fields) {
            assertEquals(fields.filter { it.contains(query, ignoreCase = true) }, index.search(query), query)
            assertEquals(fields.filter { it.equals(query, ignoreCase = true) }, index.search(query, SearchMode.Exact), query)
        }
    }

    @Test
    fun supplementaryCharactersRemainSearchableAsLiteralText() {
        val index = indexOf(["before \uD83D\uDE80 after"], ["plain"])
        assertEquals([0], index.search("\uD83D\uDE80").ids())
        assertEquals([0], index.search("\uD83D\uDE80 a").ids())
    }

    @Test
    fun selectorRunsOnlyDuringConstructionAndTextIsASnapshot() {
        var calls = 0
        val texts = mutableListOf("Button")
        val document = TestDocument(7, texts)
        val documents = mutableListOf(document)
        val index = FullTextSearch(documents) { ++calls; it.fields }
        texts[0] = "Slider"
        documents.clear()
        assertEquals(1, index.size)
        repeat(3) {
            assertSame(document, index.search("Button").single())
            assertSame(document, index.search("Button", SearchMode.WholeWord).single())
            assertSame(document, index.search("Buton", maxEditDistance = 1).single())
            assertSame(document, index.search(Regex("^Button$")).single())
            assertEquals([], index.search("Slider"))
        }
        assertEquals(1, calls)
    }

    @Test
    fun equalInputPositionsRemainSeparateDocuments() {
        val index = FullTextSearch(["Button", "Button"]) { [it] }
        assertEquals(["Button", "Button"], index.search("Button"))
    }

    @Test
    fun resultsKeepRegistrationOrderAndLimitAppliesAfterVerification() {
        val index = indexOf(["zebra"], ["Alpha"], ["alphabet"], ["alpine"])
        assertEquals([1, 2, 3], index.search("al", SearchMode.Prefix).ids())
        assertEquals([1, 2], index.search("al", SearchMode.Prefix, limit = 2).ids())
        for (mode in SearchMode.entries) assertEquals([], index.search("al", mode, limit = 0))
        assertEquals([], index.search(Regex(".*"), limit = 0))
    }

    @Test
    fun invalidLimitsAndDistancesFailBeforeSearch() {
        val index = indexOf()
        assertFailsWith<IllegalArgumentException> { SearchOptions(maxEditDistance = -1) }
        assertFailsWith<IllegalArgumentException> { SearchOptions(limit = -1) }
        assertFailsWith<IllegalArgumentException> { index.search("", limit = -1) }
        assertFailsWith<IllegalArgumentException> { index.search("", maxEditDistance = -1) }
        assertFailsWith<IllegalArgumentException> { index.search(Regex(""), limit = -1) }
    }
}
