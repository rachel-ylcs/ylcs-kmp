package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.browser.localStorage
import kotlinx.datetime.Clock
import love.yinlin.extension.Array
import love.yinlin.extension.Int
import love.yinlin.extension.JsonConverter
import love.yinlin.extension.String
import love.yinlin.extension.makeArray
import love.yinlin.extension.parseJson
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString

@Stable
actual class KV {
	private fun setItem(key: String, value: String, expire: Int) {
		val time = if (expire == KVExpire.NEVER) expire else Clock.System.now().epochSeconds.toInt() + expire
		localStorage.setItem(key, makeArray {
			add(time)
			add(value)
		}.toJsonString())
	}

	fun getItem(key: String): String? = localStorage.getItem(key)?.let { json ->
		try {
			val arr = json.parseJson.Array
			val time = arr[0].Int
			val value = arr[1].String
			val current = Clock.System.now().epochSeconds.toInt()
			if (time == KVExpire.NEVER || current <= time) value
			else {
				localStorage.removeItem(key)
				null
			}
		}
		catch (_: Throwable) { null }
	}

	actual fun set(key: String, value: Boolean, expire: Int) {
		setItem(key, value.toString(), expire)
	}

	actual fun set(key: String, value: Int, expire: Int) {
		setItem(key, value.toString(), expire)
	}

	actual fun set(key: String, value: Long, expire: Int) {
		setItem(key, value.toString(), expire)
	}

	actual fun set(key: String, value: Float, expire: Int) {
		setItem(key, value.toString(), expire)
	}

	actual fun set(key: String, value: Double, expire: Int) {
		setItem(key, value.toString(), expire)
	}

	actual fun set(key: String, value: String, expire: Int) {
		setItem(key, value, expire)
	}

	actual fun set(key: String, value: ByteArray, expire: Int) {
		setItem(key, value.toJsonString(JsonConverter.ByteArray), expire)
	}

	actual inline fun <reified T> get(key: String, default: T): T {
		val value = getItem(key)
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

	actual fun has(key: String): Boolean = getItem(key) != null

	actual fun remove(key: String) {
		localStorage.removeItem(key)
	}
}