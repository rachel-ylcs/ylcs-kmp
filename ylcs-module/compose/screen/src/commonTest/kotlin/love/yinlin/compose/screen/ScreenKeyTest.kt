package love.yinlin.compose.screen

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.extension.*
import kotlin.test.*

class ScreenKeyTest {
    @Test
    fun jsonRoundTripPreservesIdentityAndNestedArguments() {
        val args = ScreenArgs(makeArray {
            add("中文 | ? \" \\ \n")
            add(123)
            add(null)
            obj {
                "enabled" with true
                arr("items") {
                    add("a|b?c")
                }
            }
        })
        val key = ScreenKey("love.yinlin.screen.ScreenUser", args)
        val json = key.toJson().Object

        assertEquals(["type", "id", "args"], json.keys)
        assertEquals(args.toJson(), json["args"])
        assertEquals(key, ScreenKey.parse(key.toString()))
        assertEquals(key.hashCode(), ScreenKey.parse(key.toString()).hashCode())
        assertEquals(key, key.toString().parseJsonValue())
    }

    @Test
    fun identicalCreationArgumentsStillProduceDifferentEntries() {
        val first = ScreenKey("ScreenUser")
        val second = ScreenKey("ScreenUser")

        assertNotEquals(first.id, second.id)
        assertNotEquals(first, second)
        assertEquals(first.id, ScreenKey.parse(first.toString()).id)
    }

    @Test
    fun callerMutationsCannotChangeRestoredSnapshot() {
        val values: MutableList<JsonElement> = [JsonPrimitive("before")]
        (val args, val id) = ScreenKey.parse(makeObject {
            "args" with JsonArray(values)
            "id" with "550e8400-e29b-41d4-a716-446655440000"
            "type" with "ScreenUser"
        }.toJsonString())

        values[0] = JsonPrimitive("after")

        assertEquals(JsonArray([JsonPrimitive("before")]), args.toJson())
        assertEquals("550e8400-e29b-41d4-a716-446655440000", id.toString())
    }

    @Test
    fun objectFieldOrderDoesNotChangeKeyEquality() {
        val key = ScreenKey("ScreenUser")
        val reordered = makeObject {
            "args" with key.args.toJson()
            "id" with key.id.toJson()
            "type" with key.type
        }

        assertEquals(key, ScreenKey.parse(reordered.toJsonString()))
    }

    @Test
    fun unboundKeyMarkerSurvivesJsonAndDistinguishesClassLookup() {
        val typed = ScreenKey("ScreenUser", ScreenArgs.build(7))
        val unbound = typed.copy(isUnboundKey = true)
        val restored = ScreenKey.parse(unbound.toString())

        assertFalse("isUnboundKey" in typed.toJson().Object)
        assertTrue(unbound.toJson().Object["isUnboundKey"]!!.Boolean)
        assertEquals(unbound, restored)
        assertEquals(typed.id, restored.id)
        assertEquals(typed.args, restored.args)
        assertTrue(restored.isUnboundKey)
        assertNotEquals(typed, restored)
    }

    @Test
    fun malformedDescriptorsAreRejectedInsteadOfCreatingNewEntries() {
        val key = ScreenKey("ScreenUser")
        val descriptor = key.toJson().Object

        for (field in descriptor.keys) {
            assertFailsWith<SerializationException> { ScreenKey.parse(JsonObject(descriptor - field).toJsonString()) }
        }

        val invalidFields = [
            "type" to JsonPrimitive(" "),
            "type" to JsonPrimitive(123),
            "id" to JsonPrimitive("invalid-uuid"),
            "args" to JsonPrimitive("[]"),
            "args" to JsonNull,
        ]
        for ([field, value] in invalidFields) {
            assertFailsWith<IllegalArgumentException>(field) {
                ScreenKey.parse(JsonObject(descriptor + (field to value)).toJsonString())
            }
        }
        assertFailsWith<IllegalArgumentException> { ScreenKey("") }
        assertFailsWith<IllegalArgumentException> { ScreenKey.parse("[]") }
        assertFailsWith<IllegalArgumentException> { ScreenKey.parse("{") }
    }
}
