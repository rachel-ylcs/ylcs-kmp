package love.yinlin.extension

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val Json = kotlinx.serialization.json.Json {
    prettyPrint = false
    ignoreUnknownKeys = true
}

// Json fetch

val JsonElement?.Boolean: Boolean get() = (this as JsonPrimitive).boolean
val JsonElement?.BooleanNull: Boolean? get() = (this as? JsonPrimitive)?.booleanOrNull
val JsonElement?.Int: Int get() = (this as JsonPrimitive).int
val JsonElement?.IntNull: Int? get() = (this as? JsonPrimitive)?.intOrNull
val JsonElement?.Long: Long get() = (this as JsonPrimitive).long
val JsonElement?.LongNull: Long? get() = (this as? JsonPrimitive)?.longOrNull
val JsonElement?.Float: Float get() = (this as JsonPrimitive).float
val JsonElement?.FloatNull: Float? get() = (this as? JsonPrimitive)?.floatOrNull
val JsonElement?.Double: Double get() = (this as JsonPrimitive).double
val JsonElement?.DoubleNull: Double? get() = (this as? JsonPrimitive)?.doubleOrNull
val JsonElement?.String: String get() = if (this == JsonNull) error("") else (this as JsonPrimitive).content
val JsonElement?.StringNull: String? get() = if (this == JsonNull) null else (this as? JsonPrimitive)?.contentOrNull
val JsonElement?.Object: JsonObject get() = this as JsonObject
val JsonElement?.ObjectNull: JsonObject? get() = this as? JsonObject
val JsonElement?.ObjectEmpty: JsonObject get() = this as? JsonObject ?: buildJsonObject { }
val JsonElement?.Array: JsonArray get() = this as JsonArray
val JsonElement?.ArrayNull: JsonArray? get() = this as? JsonArray
val JsonElement?.ArrayEmpty: JsonArray get() = this as? JsonArray ?: buildJsonArray { }
fun JsonObject.obj(key: String): JsonObject = this[key] as JsonObject
fun JsonObject.arr(key: String): JsonArray = this[key] as JsonArray

// Json <-> Value

inline fun <reified T> T.toJson(): JsonElement = Json.encodeToJsonElement(this)
fun <T> T.toJson(serializer: SerializationStrategy<T>): JsonElement = Json.encodeToJsonElement(serializer, this)
val Boolean?.json: JsonElement get() = JsonPrimitive(this)
val Number?.json: JsonElement get() = JsonPrimitive(this)
val String?.json: JsonElement get() = JsonPrimitive(this)
val ByteArray?.json: JsonElement get() = this?.toJson(ByteArraySerializer) ?: JsonNull

inline fun <reified T> JsonElement.to(): T = Json.decodeFromJsonElement(this)
fun <T> JsonElement.to(deserializer: DeserializationStrategy<T>): T = Json.decodeFromJsonElement(deserializer, this)

// Json <-> String

val String?.parseJson: JsonElement get() = if (this == null) JsonNull else Json.parseToJsonElement(this)

// Value <-> String

inline fun <reified T> T.toJsonString(): String = Json.encodeToString(this)
fun <T> T.toJsonString(serializer: SerializationStrategy<T>): String = Json.encodeToString(serializer, this)

inline fun <reified T> String.parseJsonValue(): T = Json.decodeFromString(this)
fun <T> String.parseJsonValue(deserializer: DeserializationStrategy<T>): T = Json.decodeFromString(deserializer, this)

// Json DSL

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS)
annotation class JsonBuildDsl

@JsonBuildDsl
data class JsonArrayScope(@PublishedApi internal val builder: JsonArrayBuilder) {
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

@JsonBuildDsl
data class JsonObjectScope(@PublishedApi internal val builder: JsonObjectBuilder) {
    infix fun String.with(value: Nothing?) = builder.put(this, JsonNull)
    infix fun String.with(value: Boolean) = builder.put(this, value.json)
    infix fun String.with(value: Number) = builder.put(this, value.json)
    infix fun String.with(value: String) = builder.put(this, value.json)
    infix fun String.with(value: JsonElement) = builder.put(this, value)

    inline fun arr(key: String, init: JsonArrayScope.() -> Unit) = builder.put(key, makeArray(init))
    inline fun obj(key: String, init: JsonObjectScope.() -> Unit) = builder.put(key, makeObject(init))

    fun merge(obj: JsonObject) {
        for ((key, value) in obj) builder.put(key, value)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun makeArray(init: @JsonBuildDsl JsonArrayScope.() -> Unit): JsonArray {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return buildJsonArray { JsonArrayScope(this).init() }
}

@OptIn(ExperimentalContracts::class)
inline fun makeObject(init: @JsonBuildDsl JsonObjectScope.() -> Unit): JsonObject {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return buildJsonObject { JsonObjectScope(this).init() }
}