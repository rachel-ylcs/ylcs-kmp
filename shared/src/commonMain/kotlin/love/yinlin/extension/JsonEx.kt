package love.yinlin.extension

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonPrimitive

val JsonElement?.boolean: Boolean get() = this?.jsonPrimitive?.boolean ?: error("")
val JsonElement?.int: Int get() = this?.jsonPrimitive?.int ?: error("")
val JsonElement?.long: Long get() = this?.jsonPrimitive?.long ?: error("")
val JsonElement?.float: Float get() = this?.jsonPrimitive?.float ?: error("")
val JsonElement?.double: Double get() = this?.jsonPrimitive?.double ?: error("")
val JsonElement?.string: String get() = this?.jsonPrimitive?.content ?: error("")
val JsonElement?.obj: JsonObject get() = this?.jsonObject ?: error("")
val JsonElement?.arr: JsonArray get() = this?.jsonArray ?: error("")
fun JsonObject.obj(key: String): JsonObject = this[key]?.jsonObject ?: error("")
fun JsonObject.arr(key: String): JsonArray = this[key]?.jsonArray ?: error("")

val Boolean?.json: JsonElement get() = JsonPrimitive(this)
val Number?.json: JsonElement get() = JsonPrimitive(this)
val String.json: JsonElement get() = JsonPrimitive(this)

data class JsonArrayScope(val builder: JsonArrayBuilder) {
	fun add(value: Nothing?) = builder.add(JsonNull)
	fun add(value: Boolean) = builder.add(JsonPrimitive(value))
	fun add(value: Number) = builder.add(JsonPrimitive(value))
	fun add(value: String) = builder.add(JsonPrimitive(value))
	fun add(value: JsonElement) = builder.add(value)

	inline fun arr(init: JsonArrayScope.() -> Unit) = builder.add(makeArray(init))
	inline fun obj(init: JsonObjectScope.() -> Unit) = builder.add(makeObject(init))
}

data class JsonObjectScope(val builder: JsonObjectBuilder) {
	infix fun String.to(value: Nothing?) = builder.put(this, JsonNull)
	infix fun String.to(value: Boolean) = builder.put(this, JsonPrimitive(value))
	infix fun String.to(value: Number) = builder.put(this, JsonPrimitive(value))
	infix fun String.to(value: String) = builder.put(this, JsonPrimitive(value))
	infix fun String.to(value: JsonElement) = builder.put(this, value)

	inline fun arr(key: String, init: JsonArrayScope.() -> Unit) = builder.put(key, makeArray(init))
	inline fun obj(key: String, init: JsonObjectScope.() -> Unit) = builder.put(key, makeObject(init))
}

inline fun makeArray(init: JsonArrayScope.() -> Unit) = buildJsonArray { JsonArrayScope(this).init() }

inline fun makeObject(init: JsonObjectScope.() -> Unit) = buildJsonObject { JsonObjectScope(this).init() }