package love.yinlin.foundation.kv

import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.MMKVLogLevel
import com.tencent.mmkv.kmp.initialize
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    actual override fun close() = kv.close()

    val kv: MMKV = run {
        MMKV.initialize(logLevel = MMKVLogLevel.None)
        MMKV.defaultMMKV()
    }

    actual fun set(key: String, value: Boolean, expire: Int) { kv.encodeBool(key, value, expire.toUInt()) }
    actual fun set(key: String, value: Int, expire: Int) { kv.encodeInt(key, value, expire.toUInt()) }
    actual fun set(key: String, value: Long, expire: Int) { kv.encodeLong(key, value, expire.toUInt()) }
    actual fun set(key: String, value: Float, expire: Int) { kv.encodeFloat(key, value, expire.toUInt()) }
    actual fun set(key: String, value: Double, expire: Int) { kv.encodeDouble(key, value, expire.toUInt()) }
    actual fun set(key: String, value: String, expire: Int) { kv.encodeString(key, value, expire.toUInt()) }
    actual fun set(key: String, value: ByteArray, expire: Int) { kv.encodeBytes(key, value, expire.toUInt()) }
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> kv.decodeBool(key, default) as T
        is Int -> kv.decodeInt(key, default) as T
        is Long -> kv.decodeLong(key, default) as T
        is Float -> kv.decodeFloat(key, default) as T
        is Double -> kv.decodeDouble(key, default) as T
        is String -> (kv.decodeString(key, default) ?: default) as T
        is ByteArray -> (kv.decodeBytes(key) ?: default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = kv.containsKey(key)
    actual fun remove(key: String) = kv.removeValueForKey(key)
}
