package love.yinlin.platform

import kotlinx.serialization.json.Json

object KVExpire {
	const val NEVER = 0
	const val MINUTE = 60
	const val HOUR = 3600
	const val DAY = 86400
	const val MONTH = 2592000
	const val YEAR = 946080000
}

expect class KVContext

expect class KV(context: KVContext) {
	fun set(key: String, value: Boolean, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Int, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Long, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Float, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: Double, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: String, expire: Int = KVExpire.NEVER)
	fun set(key: String, value: ByteArray, expire: Int = KVExpire.NEVER)
	inline fun <reified T> get(key: String, default: T): T
	fun has(key: String): Boolean
}

inline fun <reified T> KV.setJson(key: String, value: T, expire: Int = KVExpire.NEVER) = set(key, Json.encodeToString(value), expire)

inline fun <reified T> KV.getJson(key: String, default: T?): T? {
	val json = get(key, "")
	return if (json.isEmpty()) default else Json.decodeFromString<T>(json)
}