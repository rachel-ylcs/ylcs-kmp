package love.yinlin.extension

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
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