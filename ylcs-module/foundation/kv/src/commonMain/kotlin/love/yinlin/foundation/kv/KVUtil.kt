package love.yinlin.foundation.kv

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString

inline fun <reified T> KV.setJson(key: String, value: T, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(), expire) }

fun <T> KV.setJson(serializer: SerializationStrategy<T>, key: String, value: T, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(serializer), expire) }

inline fun <reified T> KV.getJson(key: String, defaultFactory: () -> T): T = catchingDefault({ defaultFactory() }) {
    val json = get(key, "")
    require(json.isNotEmpty())
    json.parseJsonValue()
}

inline fun <T> KV.getJson(deserializer: DeserializationStrategy<T>, key: String, defaultFactory: () -> T): T = catchingDefault({ defaultFactory() }) {
    val json = get(key, "")
    require(json.isNotEmpty())
    json.parseJsonValue(deserializer)
}
