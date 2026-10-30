package love.yinlin.foundation.kv

import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    actual override fun close() = kv.close()

    val kv: MMKV = run {
        MMKV.initialize(context, MMKVLogLevel.LevelNone)
        MMKV.defaultMMKV()
    }

    actual fun set(key: String, value: Boolean, expire: Int) { kv.encode(key, value, expire) }
    actual fun set(key: String, value: Int, expire: Int) { kv.encode(key, value, expire) }
    actual fun set(key: String, value: Long, expire: Int) { kv.encode(key, value, expire) }
    actual fun set(key: String, value: Float, expire: Int) { kv.encode(key, value, expire) }
    actual fun set(key: String, value: Double, expire: Int) { kv.encode(key, value, expire) }
    actual fun set(key: String, value: String, expire: Int) { kv.encode(key, value, expire) }
    actual fun set(key: String, value: ByteArray, expire: Int) { kv.encode(key, value, expire) }
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> kv.decodeBool(key, default) as T
        is Int -> kv.decodeInt(key, default) as T
        is Long -> kv.decodeLong(key, default) as T
        is Float -> kv.decodeFloat(key, default) as T
        is Double -> kv.decodeDouble(key, default) as T
        is String -> kv.decodeString(key, default) as T
        is ByteArray -> kv.decodeBytes(key, default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = kv.containsKey(key)
    actual fun remove(key: String) = kv.removeValueForKey(key)
}
