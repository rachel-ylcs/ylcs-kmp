package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.service

@Stable
actual class KV {
	val handle: Long = init(Path(service.os.storage.dataPath, "config").toString())

	actual fun set(key: String, value: Boolean, expire: Int) = setBoolean(handle, key, value, expire)
	actual fun set(key: String, value: Int, expire: Int) = setInt(handle, key, value, expire)
	actual fun set(key: String, value: Long, expire: Int) = setLong(handle, key, value, expire)
	actual fun set(key: String, value: Float, expire: Int) = setFloat(handle, key, value, expire)
	actual fun set(key: String, value: Double, expire: Int) = setDouble(handle, key, value, expire)
	actual fun set(key: String, value: String, expire: Int) = setString(handle, key, value, expire)
	actual fun set(key: String, value: ByteArray, expire: Int) = setByteArray(handle, key, value, expire)

	actual inline fun <reified T> get(key: String, default: T): T {
		return when (default) {
			is Boolean -> getBoolean(handle, key, default) as T
			is Int -> getInt(handle, key, default) as T
			is Long -> getLong(handle, key, default) as T
			is Float -> getFloat(handle, key, default) as T
			is Double -> getDouble(handle, key, default) as T
			is String -> getString(handle, key, default) as T
			is ByteArray -> getByteArray(handle, key, default) as T
			else -> default
		}
	}

	actual fun has(key: String): Boolean = has(handle, key)
	actual fun remove(key: String) = remove(handle, key)

	private external fun init(path: String): Long
	private external fun setBoolean(handle: Long, key: String, value: Boolean, expire: Int)
	private external fun setInt(handle: Long, key: String, value: Int, expire: Int)
	private external fun setLong(handle: Long, key: String, value: Long, expire: Int)
	private external fun setFloat(handle: Long, key: String, value: Float, expire: Int)
	private external fun setDouble(handle: Long, key: String, value: Double, expire: Int)
	private external fun setString(handle: Long, key: String, value: String, expire: Int)
	private external fun setByteArray(handle: Long, key: String, value: ByteArray, expire: Int)
	external fun getBoolean(handle: Long, key: String, default: Boolean): Boolean
	external fun getInt(handle: Long, key: String, default: Int): Int
	external fun getLong(handle: Long, key: String, default: Long): Long
	external fun getFloat(handle: Long, key: String, default: Float): Float
	external fun getDouble(handle: Long, key: String, default: Double): Double
	external fun getString(handle: Long, key: String, default: String): String
	external fun getByteArray(handle: Long, key: String, default: ByteArray): ByteArray
	private external fun has(handle: Long, key: String): Boolean
	private external fun remove(handle: Long, key: String)
}