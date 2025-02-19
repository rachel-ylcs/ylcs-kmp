package love.yinlin.extension

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

val Json = Json {
	prettyPrint = false
	ignoreUnknownKeys = true
}

val JsonElement?.Boolean: Boolean get() = (this as? JsonPrimitive)?.boolean ?: error("")
val JsonElement?.BooleanNull: Boolean? get() = (this as? JsonPrimitive)?.booleanOrNull
val JsonElement?.Int: Int get() = (this as? JsonPrimitive)?.int ?: error("")
val JsonElement?.IntNull: Int? get() = (this as? JsonPrimitive)?.intOrNull
val JsonElement?.Long: Long get() = (this as? JsonPrimitive)?.long ?: error("")
val JsonElement?.LongNull: Long? get() = (this as? JsonPrimitive)?.longOrNull
val JsonElement?.Float: Float get() = (this as? JsonPrimitive)?.float ?: error("")
val JsonElement?.FloatNull: Float? get() = (this as? JsonPrimitive)?.floatOrNull
val JsonElement?.Double: Double get() = (this as? JsonPrimitive)?.double ?: error("")
val JsonElement?.DoubleNull: Double? get() = (this as? JsonPrimitive)?.doubleOrNull
val JsonElement?.String: String get() = (this as? JsonPrimitive)?.content ?: error("")
val JsonElement?.StringNull: String? get() = (this as? JsonPrimitive)?.contentOrNull
@OptIn(ExperimentalStdlibApi::class)
val JsonElement?.ByteArray: ByteArray get() = (this as? JsonPrimitive)?.content?.hexToByteArray(HexFormat.UpperCase) ?: error("")
@OptIn(ExperimentalStdlibApi::class)
val JsonElement?.ByteArrayEmpty: ByteArray get() = (this as? JsonPrimitive)?.contentOrNull?.hexToByteArray(HexFormat.UpperCase) ?: byteArrayOf()
val JsonElement?.Object: JsonObject get() = (this as? JsonObject) ?: error("")
val JsonElement?.ObjectNull: JsonObject? get() = this as? JsonObject
val JsonElement?.ObjectEmpty: JsonObject get() = (this as? JsonObject) ?: buildJsonObject { }
val JsonElement?.Array: JsonArray get() = (this as? JsonArray) ?: error("")
val JsonElement?.ArrayEmpty: JsonArray get() = (this as? JsonArray) ?: buildJsonArray { }
fun JsonObject.obj(key: String): JsonObject = (this[key] as? JsonObject) ?: error("")
fun JsonObject.arr(key: String): JsonArray = (this[key] as? JsonArray) ?: error("")

val Boolean?.json: JsonElement get() = JsonPrimitive(this)
val Number?.json: JsonElement get() = JsonPrimitive(this)
val String?.json: JsonElement get() = JsonPrimitive(this)
@OptIn(ExperimentalStdlibApi::class)
val ByteArray?.json: JsonElement get() = JsonPrimitive(this?.toHexString(HexFormat.UpperCase))

inline fun <reified T> T?.toJsonString() : String = if (this == null) "null" else Json.encodeToString(this)

data class JsonArrayScope(val builder: JsonArrayBuilder) {
	fun add(value: Nothing?) = builder.add(JsonNull)
	fun add(value: Boolean) = builder.add(value.json)
	fun add(value: Number) = builder.add(value.json)
	fun add(value: String) = builder.add(value.json)
	fun add(value: JsonElement) = builder.add(value)

	inline fun arr(init: JsonArrayScope.() -> Unit) = builder.add(makeArray(init))
	inline fun obj(init: JsonObjectScope.() -> Unit) = builder.add(makeObject(init))

	fun merge(arr: JsonArray) {
		for (value in arr) builder.add(value)
	}
}

data class JsonObjectScope(val builder: JsonObjectBuilder) {
	infix fun String.to(value: Nothing?) = builder.put(this, JsonNull)
	infix fun String.to(value: Boolean) = builder.put(this, value.json)
	infix fun String.to(value: Number) = builder.put(this, value.json)
	infix fun String.to(value: String) = builder.put(this, value.json)
	infix fun String.to(value: JsonElement) = builder.put(this, value)

	inline fun arr(key: String, init: JsonArrayScope.() -> Unit) = builder.put(key, makeArray(init))
	inline fun obj(key: String, init: JsonObjectScope.() -> Unit) = builder.put(key, makeObject(init))

	fun merge(obj: JsonObject) {
		for ((key, value) in obj) builder.put(key, value)
	}
}

inline fun makeArray(init: JsonArrayScope.() -> Unit) = buildJsonArray { JsonArrayScope(this).init() }

inline fun makeObject(init: JsonObjectScope.() -> Unit) = buildJsonObject { JsonObjectScope(this).init() }