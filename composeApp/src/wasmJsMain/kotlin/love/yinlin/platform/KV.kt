package love.yinlin.platform

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

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
		localStorage.setItem(key, Json.encodeToString(value))
	}

	actual inline fun <reified T> get(key: String, default: T): T {
		val value = localStorage.getItem(key)
		return if (value == null) default else {
			if (default is String) value as T
			else {
				try {
					Json.decodeFromString(value)
				}
				catch (_: Exception) {
					default
				}
			}
		}
	}

	actual fun has(key: String): Boolean = localStorage.getItem(key) != null

	actual fun remove(key: String) {
		localStorage.removeItem(key)
	}
}