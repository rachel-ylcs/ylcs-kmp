package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.browser.localStorage
import love.yinlin.extension.JsonConverter
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString

@Stable
actual class KV {
	actual fun set(key: String, value: Boolean, expire: Int) {
		localStorage.setItem(key, value.toString())
	}

	actual fun set(key: String, value: Int, expire: Int) {
		localStorage.setItem(key, value.toString())
	}

	actual fun set(key: String, value: Long, expire: Int) {
		localStorage.setItem(key, value.toString())
	}

	actual fun set(key: String, value: Float, expire: Int) {
		localStorage.setItem(key, value.toString())
	}

	actual fun set(key: String, value: Double, expire: Int) {
		localStorage.setItem(key, value.toString())
	}

	actual fun set(key: String, value: String, expire: Int) {
		localStorage.setItem(key, value)
	}

	actual fun set(key: String, value: ByteArray, expire: Int) {
		localStorage.setItem(key, value.toJsonString(JsonConverter.ByteArray))
	}

	actual inline fun <reified T> get(key: String, default: T): T {
		val value = localStorage.getItem(key)
		return if (value == null) default else when (default) {
			is Boolean -> value as T
			is Int -> value as T
			is Long -> value as T
			is Float -> value as T
			is Double -> value as T
			is String -> value as T
			is ByteArray -> value.parseJsonValue(JsonConverter.ByteArray) as? T ?: default
			else -> default
		}
	}

	actual fun has(key: String): Boolean = localStorage.getItem(key) != null

	actual fun remove(key: String) {
		localStorage.removeItem(key)
	}
}