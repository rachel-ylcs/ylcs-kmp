package love.yinlin.foundation.fts

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PostingsTest {
    @Test
    fun sparseCursorSeeksAcrossLargeGapsWithoutOverflow() {
        val cursor = postingOf([0, 31, 32, 1000, 1_000_000_000, Int.MAX_VALUE - 1]).cursor()
        assertEquals(0, cursor.advanceTo(0))
        assertEquals(31, cursor.advanceTo(1))
        assertEquals(31, cursor.advanceTo(31))
        assertEquals(32, cursor.next())
        assertEquals(1_000_000_000, cursor.advanceTo(1001))
        assertEquals(Int.MAX_VALUE - 1, cursor.next())
        assertEquals(DocumentCursor.END_DOCUMENT, cursor.next())
        assertEquals(DocumentCursor.END_DOCUMENT, cursor.next())
    }

    @Test
    fun bitmapCursorHandlesSignBitsEmptyWordsAndRepeatedSeeks() {
        val values = ((0 .. 70).toList() + (160 .. 200).toList()).toIntArray()
        val posting = postingOf(values)
        assertTrue(posting.bitmap)
        val cursor = posting.cursor()
        for (target in [0, 1, 30, 31, 32, 63, 64, 70, 71, 160, 161, 191, 192, 200, 201]) {
            assertEquals(values.firstOrNull { it >= target } ?: DocumentCursor.END_DOCUMENT, cursor.advanceTo(target), target.toString())
        }
        assertEquals(DocumentCursor.END_DOCUMENT, cursor.next())
        assertContentEquals(values, posting.cursor().documentIds())
    }

    @Test
    fun bitmapAndCursorCanSeekAndContinueAtWordBoundaries() {
        val left = postingOf((0 .. 300).filter { it !in 80 ..< 160 }.toIntArray())
        val right = postingOf((0 .. 300).filter { it % 3 == 0 }.toIntArray())
        val expected = (0 .. 300).filter { (it !in 80 ..< 160) && it % 3 == 0 }
        val cursor = DocumentCursor.intersect([left, right])
        for (target in [0, 31, 32, 64, 80, 160, 192, 299, 301]) {
            assertEquals(expected.firstOrNull { it >= target } ?: DocumentCursor.END_DOCUMENT, cursor.advanceTo(target))
        }
        assertContentEquals(expected.toIntArray(), DocumentCursor.intersect([left, right]).documentIds())
    }

    @Test
    fun mixedIntersectionNeverDuplicatesOrSkipsDocuments() {
        val sparse = postingOf([0, 31, 32, 63, 64, 511, 1000, 10000])
        val dense = postingOf((0 .. 1100).filter { it != 31 && it != 511 }.toIntArray())
        assertContentEquals([0, 32, 63, 64, 1000], DocumentCursor.intersect([sparse, dense]).documentIds())
        assertContentEquals([0, 32, 63, 64, 1000], DocumentCursor.intersect([dense, sparse]).documentIds())
    }

    @Test
    fun sparseHeapUnionSeeksAndMergesDuplicateDocuments() {
        val lists = [intArrayOf(0, 1000, 20000), [0, 1001, 20000], [31, 1000, 30000]].map(::postingOf)
        val cursor = DocumentCursor.union(lists)
        assertEquals(0, cursor.next())
        assertEquals(31, cursor.next())
        assertEquals(1000, cursor.advanceTo(32))
        assertEquals(1001, cursor.next())
        assertEquals(30_000, cursor.advanceTo(20_001))
        assertEquals(DocumentCursor.END_DOCUMENT, cursor.next())
    }

    @Test
    fun denseUnionCombinesSparseAndBitmapInputs() {
        val lists = [(0 .. 100).toList(), [31, 127, 160], [0, 127, 191], [32, 192, 255]]
        val expected = lists.flatten().distinct().sorted().toIntArray()
        assertContentEquals(expected, DocumentCursor.union(lists.map { postingOf(it.toIntArray()) }).documentIds())
        assertContentEquals([], DocumentCursor.union([postingOf([]), postingOf([])]).documentIds())
    }

    @Test
    fun identicalFrozenPostingsShareStorage() {
        val interner = PostingInterner()
        val sparse = interner.intern(postingOf([4, 1000]))
        assertSame(sparse, interner.intern(postingOf([4, 1000])))
        val dense = interner.intern(postingOf(IntArray(2500) { it }))
        assertSame(dense, interner.intern(postingOf(IntArray(2500) { it })))
    }

    @Test
    fun randomCursorSeeksAgreeWithSortedSets() {
        val random = Random(9157)
        repeat(200) {
            val values = List(random.nextInt(1, 400)) { random.nextInt(0, 3000) }.distinct().sorted().toIntArray()
            val cursor = postingOf(values).cursor()
            val targets = List(30) { random.nextInt(0, 3100) }.sorted()
            for (target in targets) assertEquals(values.firstOrNull { it >= target } ?: DocumentCursor.END_DOCUMENT, cursor.advanceTo(target))
        }
    }

    @Test
    fun primitiveGramKeysPreserveAllUtf16BitsAndFieldBoundaries() {
        val random = Random(440)
        val alphabet: CharArray = ['\u0000', '\uFFFF', '\u8000', '\u007F', '\uD800', '\uDC00', 'A', 'Σ', '中', '_']
        val texts = List(80) { buildString { repeat(25) { append(alphabet.random(random)) } } }
        val documents = texts.chunked(2).mapIndexed { id, fields -> TestDocument(id, fields) }
        val index = FullTextSearch(documents) { it.fields }
        val queries = texts.take(40).flatMap { [it.take(1), it.take(2), it.take(3), it.take(12), it.takeLast(15)] }
        for (query in queries) for (ignoreCase in [false, true]) {
            val expected = documents.filter { it.fields.any { field -> field.contains(query, ignoreCase) } }
            assertEquals(expected, index.search(query, ignoreCase))
        }
    }

    @Test
    fun mergedPersistentPostingsAgreeWithSetUnion() {
        val random = Random(871)
        repeat(200) {
            val first = List(random.nextInt(0, 500)) { random.nextInt(0, 1500) }.distinct().sorted().toIntArray()
            val second = List(random.nextInt(0, 500)) { random.nextInt(0, 1500) }.distinct().sorted().toIntArray()
            val expected = (first.toList() + second.toList()).distinct().sorted().toIntArray()
            assertContentEquals(expected, PostingList.mergePostings(postingOf(first), postingOf(second)).cursor().documentIds())
        }
        assertContentEquals([0, Int.MAX_VALUE - 1], PostingList.mergePostings(postingOf([0]), postingOf([Int.MAX_VALUE - 1])).cursor().documentIds())
    }
}
