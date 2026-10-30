@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.foundation.kv

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.love.yinlin.foundation.ylcs.module.foundation.kv.*
import love.yinlin.extension.toNSData
import love.yinlin.extension.toByteArray
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    actual override fun close() = kv.close()

    // MMKV initialized in Swift code
    val kv: MMKV = run {
        MMKV.initializeMMKV(null, 4UL) // MMKVLogLevel.LevelNone
        MMKV.defaultMMKV()!!
    }

    actual fun set(key: String, value: Boolean, expire: Int)  { kv.setBool(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Int, expire: Int) { kv.setInt32(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Long, expire: Int) { kv.setInt64(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Float, expire: Int) { kv.setFloat(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Double, expire: Int) { kv.setDouble(value, key, expire.toUInt()) }
    actual fun set(key: String, value: String, expire: Int) { kv.setString(value, key, expire.toUInt()) }
    actual fun set(key: String, value: ByteArray, expire: Int) { kv.setData(value.toNSData(), key, expire.toUInt()) }
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> kv.getBoolForKey(key, default) as T
        is Int -> kv.getInt32ForKey(key, default) as T
        is Long -> kv.getInt64ForKey(key, default) as T
        is Float -> kv.getFloatForKey(key, default) as T
        is Double -> kv.getDoubleForKey(key, default) as T
        is String -> kv.getStringForKey(key, default) as T
        is ByteArray -> (kv.getDataForKey(key, default.toNSData())?.toByteArray() ?: default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = kv.containsKey(key)
    actual fun remove(key: String) { kv.removeValueForKey(key) }
}
