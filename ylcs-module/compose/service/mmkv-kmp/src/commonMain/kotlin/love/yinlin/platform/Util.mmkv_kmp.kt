package love.yinlin.platform

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.startup.StartupKV

inline fun <reified T> StartupKV.setJson(key: String, value: T, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(), expire) }

fun <T> StartupKV.setJson(serializer: SerializationStrategy<T>, key: String, value: T, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(serializer), expire) }

inline fun <reified T> StartupKV.getJson(key: String, defaultFactory: () -> T): T = catchingDefault({ defaultFactory() }) {
    val json = get(key, "")
    require(json.isNotEmpty())
    json.parseJsonValue()
}

fun <T> StartupKV.getJson(deserializer: DeserializationStrategy<T>, key: String, defaultFactory: () -> T): T = catchingDefault({ defaultFactory() }) {
    val json = get(key, "")
    require(json.isNotEmpty())
    json.parseJsonValue(deserializer)
}