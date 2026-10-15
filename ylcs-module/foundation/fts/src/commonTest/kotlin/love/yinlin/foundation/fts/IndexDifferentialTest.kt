package love.yinlin.foundation.fts

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal fun postingOf(values: IntArray): PostingList = PostingBuilder().also { builder -> values.forEach(builder::add) }.build()

internal fun DocumentCursor.documentIds(): IntArray {
    val result = ArrayList<Int>()
    var document = next()
    while (document != DocumentCursor.END_DOCUMENT) {
        result.add(document)
        document = next()
    }
    return result.toIntArray()
}

class IndexDifferentialTest {
    // 独立、无剪枝的参考算法；仅用于小规模测试，不复用索引或带状 DP。
    private fun distance(first: String, second: String, ignoreCase: Boolean): Int {
        var previous = IntArray(second.length + 1) { it }
        for (left in first.indices) {
            val row = IntArray(second.length + 1)
            row[0] = left + 1
            for (right in second.indices) {
                row[right + 1] = minOf(
                    previous[right + 1] + 1,
                    row[right] + 1,
                    previous[right] + if (first[left].equals(second[right], ignoreCase)) 0 else 1,
                )
            }
            previous = row
        }
        return previous.last()
    }

    private fun Random.word(): String = buildString {
        repeat(nextInt(1, 10)) { append("aAbBcC012_"[nextInt(10)]) }
    }

    private fun words(text: String): List<String> = text.split(Regex("[^a-zA-Z0-9_]+")).filter { it.isNotEmpty() }

    @Test
    fun literalModesAgreeWithLinearSearchForRandomTextAndLimits() {
        val random = Random(90210)
        val alphabet = "aAbB ._/中文Σςİı012"
        fun text(): String = buildString { repeat(random.nextInt(0, 24)) { append(alphabet[random.nextInt(alphabet.length)]) } }
        val documents = List(100) { id -> TestDocument(id, List(random.nextInt(0, 4)) { text() }) }
        val index = FullTextSearch(documents) { it.fields }
        val queries = List(100) { text() } + documents.flatMap { it.fields }.take(40).flatMap { [it, it.take(1), it.take(2), it.take(4)] }
        for (query in queries) for (ignoreCase in [false, true]) for (mode in [SearchMode.Contains, SearchMode.Exact]) {
            val limit = random.nextInt(0, 110)
            val expected = documents.filter { document ->
                query.isEmpty() || document.fields.any {
                    if (mode == SearchMode.Exact) it.equals(query, ignoreCase) else it.contains(query, ignoreCase)
                }
            }.take(limit)
            assertEquals(expected, index.search(query, mode, ignoreCase, limit), "$mode / $ignoreCase / '$query'")
        }
    }

    @Test
    fun tokenModesAgreeWithIndependentLinearMatching() {
        val random = Random(743)
        val vocabulary = List(60) { random.word() }
        val documents = List(90) { id ->
            TestDocument(id, List(random.nextInt(0, 4)) { List(random.nextInt(0, 5)) { vocabulary.random(random) }.joinToString(" / ") })
        }
        val tokens = documents.map { document -> document.fields.flatMap(::words) }
        val index = FullTextSearch(documents) { it.fields }
        val queries = buildList {
            addAll(vocabulary)
            vocabulary.take(20).mapTo(this) { it.take(2) }
            addAll(List(30) {
                [vocabulary.random(random), vocabulary.random(random)].joinToString(" ")
            })
            addAll(["", "!!!", "unfindable"])
        }
        for (query in queries) for (ignoreCase in [false, true]) {
            val queryWords = words(query)
            for (mode in [SearchMode.WholeWord, SearchMode.Prefix, SearchMode.Fuzzy]) {
                val edits = random.nextInt(0, 4)
                val limit = random.nextInt(0, 100)
                val expected = documents.filterIndexed { document, _ ->
                    query.isEmpty() || (queryWords.isNotEmpty() && queryWords.all { word ->
                        tokens[document].any { term ->
                            when (mode) {
                                SearchMode.WholeWord -> term.equals(word, ignoreCase)
                                SearchMode.Prefix -> term.startsWith(word, ignoreCase)
                                else -> distance(word, term, ignoreCase) <= edits
                            }
                        }
                    })
                }.take(limit)
                val options = SearchOptions(mode, ignoreCase, edits, limit)
                assertEquals(expected, index.search(query, options), "$mode / $ignoreCase / $edits / '$query'")
            }
        }
    }

    @Test
    fun unicodeFuzzyResultsAgreeWithCharacterComparison() {
        val words = ["K", "K", "ſ", "S", "ς", "Σ", "σ", "I", "İ", "ı", "i", "ß", "SS", "中文", "中"]
        val index = FullTextSearch(words) { [it] }
        for (query in words) for (ignoreCase in [false, true]) for (edits in 0 .. 2) {
            assertEquals(words.filter { distance(query, it, ignoreCase) <= edits }, index.search(query, edits, ignoreCase), "$query / $ignoreCase / $edits")
        }
    }

    @Test
    fun bandedEditDistanceAgreesWithFullDynamicProgramming() {
        val random = Random(1729)
        repeat(500) {
            val query = if (random.nextBoolean()) random.word() else ""
            val text = if (random.nextBoolean()) random.word() else ""
            for (edits in 0 .. 4) assertEquals(distance(query, text, false) <= edits, EditDistance(query, edits).matches(text), "$query / $text / $edits")
        }
    }

    @Test
    fun postingUnionAndIntersectionAgreeWithSetOperations() {
        val random = Random(811)
        repeat(200) {
            val lists = List(random.nextInt(0, 20)) {
                List(random.nextInt(0, 80)) { random.nextInt(0, 200) }.distinct().sorted().toIntArray()
            }
            val expectedUnion = lists.flatMap { it.toList() }.distinct().sorted().toIntArray()
            val expectedIntersection = if (lists.isEmpty()) [] else lists.drop(1)
                .fold(lists.first().toSet()) { result, list -> result.intersect(list.toSet()) }.sorted().toIntArray()
            assertContentEquals(expectedUnion, DocumentCursor.union(lists.map(::postingOf)).documentIds())
            assertContentEquals(expectedIntersection, DocumentCursor.intersect(lists.map(::postingOf)).documentIds())
        }
    }

    @Test
    fun sparseIntersectionSkipsLongPostingsWithoutMissingEndpoints() {
        val dense = IntArray(10_000) { it * 2 }
        val sparse: IntArray = [0, 1, 98, 100, 19998, 20000]
        assertContentEquals([0, 98, 100, 19998], DocumentCursor.intersect([sparse, dense].map(::postingOf)).documentIds())
    }
}
