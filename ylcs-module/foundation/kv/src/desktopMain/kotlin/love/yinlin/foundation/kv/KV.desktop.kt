package love.yinlin.foundation.kv

import love.yinlin.foundation.NativeLibApi
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

@NativeLibApi
actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    actual override fun close() = kv.close()

    val kv: NativeKV = NativeKV(initPath.path)

    actual fun set(key: String, value: Boolean, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Int, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Long, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Float, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Double, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: String, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: ByteArray, expire: Int) = kv.set(key, value, expire)
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> kv.getBoolean(key, default) as T
        is Int -> kv.getInt(key, default) as T
        is Long -> kv.getLong(key, default) as T
        is Float -> kv.getFloat(key, default) as T
        is Double -> kv.getDouble(key, default) as T
        is String -> kv.getString(key, default) as T
        is ByteArray -> kv.getByteArray(key, default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = kv.contains(key)
    actual fun remove(key: String) = kv.remove(key)
}
