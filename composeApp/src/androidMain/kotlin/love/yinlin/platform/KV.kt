package love.yinlin.platform

import android.content.Context
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel

actual class KV(context: Context) {
	val kv: MMKV = run {
		MMKV.initialize(context, MMKVLogLevel.LevelNone)
		MMKV.defaultMMKV()
	}

	actual fun set(key: String, value: Boolean, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual fun set(key: String, value: Int, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual fun set(key: String, value: Long, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual fun set(key: String, value: Float, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual fun set(key: String, value: Double, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual fun set(key: String, value: String, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual fun set(key: String, value: ByteArray, expire: Int) {
		kv.encode(key, value, expire)
	}

	actual inline fun <reified T> get(key: String, default: T): T {
		return when (default) {
			is Boolean -> kv.decodeBool(key, default) as T
			is Int -> kv.decodeInt(key, default) as T
			is Long -> kv.decodeLong(key, default) as T
			is Float -> kv.decodeFloat(key, default) as T
			is Double -> kv.decodeDouble(key, default) as T
			is String -> kv.decodeString(key, default) as T
			is ByteArray -> kv.decodeBytes(key, default) as T
			else -> default
		}
	}

	actual fun has(key: String): Boolean = kv.containsKey(key)

	actual fun remove(key: String) {
		kv.removeValueForKey(key)
	}
}