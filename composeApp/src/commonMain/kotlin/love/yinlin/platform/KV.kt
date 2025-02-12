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

inline fun <reified T> KV.setJson(key: String, value: T?, expire: Int = KVExpire.NEVER) {
	try {
		set(key, Json.encodeToString(value), expire)
	}
	catch (_: Exception) { }
}

inline fun <reified T> KV.getJson(key: String, default: T): T {
	return try {
		val json = get(key, "")
		if (json.isEmpty()) default else Json.decodeFromString(json)
	}
	catch (_: Exception) {
		default
	}
}