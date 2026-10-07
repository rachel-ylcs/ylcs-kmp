package love.yinlin.compose.screen

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import love.yinlin.extension.parseJsonValue
import kotlin.test.*

class ScreenArgsTest {
    @Serializable
    private data class Payload(val id: Long, val title: String, val tags: List<String>)

    @Serializable
    private enum class Filter { Favorites }

    @Test
    fun buildOneToFiveArgumentsPreservesOrderAndTypes() {
        val largeId = 9007199254740993L
        val cases: List<ScreenArgs> = [
            ScreenArgs.build("first"),
            ScreenArgs.build("first", 2),
            ScreenArgs.build("first", 2, true),
            ScreenArgs.build("first", 2, true, largeId),
            ScreenArgs.build("first", 2, true, largeId, 2.5),
        ]

        for ([index, args] in cases.withIndex()) {
            assertEquals(index + 1, args.size)
            assertFalse(args.isEmpty)
            assertTrue(args.isNotEmpty)
            assertEquals("first", args[0])
            if (index >= 1) assertEquals(2, args[1])
            if (index >= 2) assertEquals(true, args[2])
            if (index >= 3) assertEquals(largeId, args[3])
            if (index >= 4) assertEquals(2.5, args[4])
        }
    }

    @Test
    fun nullableArgumentsKeepTheirPositions() {
        val args = ScreenArgs.build<String?, Long?, Payload?, Boolean?, List<Int>?>(null, 31L, null, true, null)

        assertEquals(5, args.size)
        assertNull(args.get<String?>(0))
        assertEquals(31L, args.get<Long?>(1))
        assertNull(args.get<Payload?>(2))
        assertEquals(true, args.get<Boolean?>(3))
        assertNull(args.get<List<Int>?>(4))
    }

    @Test
    fun serializableObjectsEnumsAndCollectionsRoundTrip() {
        val payload = Payload(42L, "中文 | ?", ["first", "second"])
        val items: List<Payload> = [payload]
        val lookup: Map<String, Payload> = mapOf("main" to payload)
        @Suppress("RedundantNullableReturnType")
        val optional: Payload? = payload
        val args = ScreenArgs.build(payload, Filter.Favorites, items, lookup, optional)

        assertEquals(payload, args[0])
        assertEquals(Filter.Favorites, args[1])
        assertEquals(items, args[2])
        assertEquals(lookup, args[3])
        assertEquals(optional, args[4])
    }

    @Test
    fun builtArgumentsAreDetachedFromMutableSources() {
        val tags: MutableList<String> = ["before"]
        val ids: MutableList<Long> = [10L, 20L]
        val payload = Payload(1L, "title", tags)
        val sourceIds: List<Long> = ids
        val args = ScreenArgs.build(payload, sourceIds)
        val originalJson = args.toString()
        val originalHash = args.hashCode()

        tags[0] = "after"
        ids += 30L

        assertEquals(Payload(1L, "title", ["before"]), args[0])
        assertEquals([10L, 20L], args[1])
        assertEquals(originalJson, args.toString())
        assertEquals(originalHash, args.hashCode())
    }

    @Test
    fun jsonRoundTripPreservesEqualityHashCodeAndValues() {
        val payload = Payload(Long.MAX_VALUE, "中文 | ? \" \\ \n", ["a", "b"])
        val args = ScreenArgs.build<Payload, String?, Boolean, List<Int>, Long>(payload, null, true, [1, 2], Long.MIN_VALUE)
        val restored: ScreenArgs = args.toString().parseJsonValue()

        assertEquals(args, restored)
        assertEquals(args.hashCode(), restored.hashCode())
        assertEquals(args.toString(), restored.toString())
        assertEquals(payload, restored[0])
        assertNull(restored.get<String?>(1))
        assertEquals(true, restored[2])
        assertEquals([1, 2], restored[3])
        assertEquals(Long.MIN_VALUE, restored[4])
    }

    @Test
    fun emptyArgumentsSerializeAsAnEmptyArray() {
        val args = ScreenArgs.Empty
        val restored: ScreenArgs = "[]".parseJsonValue()

        assertEquals(0, args.size)
        assertTrue(args.isEmpty)
        assertFalse(args.isNotEmpty)
        assertEquals("[]", args.toString())
        assertEquals(args, restored)
        assertEquals(args.hashCode(), restored.hashCode())
    }

    @Test
    fun nullIsRejectedWhenReadingANonNullableType() {
        val args = ScreenArgs.build<String?>(null)

        assertNull(args.get<String?>(0))
        assertFailsWith<SerializationException> { args.get<String>(0) }
    }

    @Test
    fun incompatibleArgumentTypesFailDecoding() {
        val args = ScreenArgs.build("not-a-number")

        assertFailsWith<SerializationException> { args.get<Long>(0) }
        assertEquals("not-a-number", args[0])
    }
}
