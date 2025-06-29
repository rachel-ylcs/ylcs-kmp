package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString

// MMKV 过期时间单位是秒
object KVExpire {
	const val NEVER = 0
	const val MINUTE = 60
	const val HOUR = 3600
	const val DAY = 86400
	const val MONTH = 2592000
	const val YEAR = 946080000
}

@Stable
expect class KV {
	fun set(key: String, value: Boolean, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Int, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Long, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Float, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Double, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: String, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: ByteArray, expire: Int = KVExpire.NEVER)
	inline fun <reified T> get(key: String, default: T): T
	fun has(key: String): Boolean
	fun remove(key: String)
}

inline fun <reified T> KV.setJson(key: String, value: T?, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(), expire) }

fun <T> KV.setJson(serializer: SerializationStrategy<T>, key: String, value: T?, expire: Int = KVExpire.NEVER) = catching { set(key, value.toJsonString(serializer), expire) }

inline fun <reified T> KV.getJson(key: String, defaultFactory: () -> T): T = catchingDefault(defaultFactory) {
	val json = get(key, "")
	require(json.isNotEmpty())
	json.parseJsonValue()!!
}

fun <T> KV.getJson(deserializer: DeserializationStrategy<T>, key: String, defaultFactory: () -> T): T = catchingDefault(defaultFactory) {
	val json = get(key, "")
	require(json.isNotEmpty())
	json.parseJsonValue(deserializer)!!
}