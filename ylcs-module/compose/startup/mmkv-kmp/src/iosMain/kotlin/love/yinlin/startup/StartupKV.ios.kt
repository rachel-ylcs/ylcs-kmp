package love.yinlin.startup

import cocoapods.MMKV.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import love.yinlin.extension.toNSData
import love.yinlin.extension.toByteArray
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup
import love.yinlin.fs.File

@StartupArg(index = 0, name = "initPath", type = File::class)
@OptIn(ExperimentalForeignApi::class)
actual class StartupKV actual constructor(context: PlatformContextProvider): SyncStartup(context) {
    // MMKV initialized in swift code
    val mmkv: MMKV = run {
        MMKV.initializeMMKV(null, MMKVLogLevel.None)
        MMKV.defaultMMKV()!!
    }

    actual override fun init(scope: CoroutineScope, args: StartupArgs) { }

    actual fun set(key: String, value: Boolean, expire: Int)  { mmkv.setBool(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Int, expire: Int) { mmkv.setInt32(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Long, expire: Int) { mmkv.setInt64(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Float, expire: Int) { mmkv.setFloat(value, key, expire.toUInt()) }
    actual fun set(key: String, value: Double, expire: Int) { mmkv.setDouble(value, key, expire.toUInt()) }
    actual fun set(key: String, value: String, expire: Int) { mmkv.setString(value, key, expire.toUInt()) }
    actual fun set(key: String, value: ByteArray, expire: Int) { mmkv.setData(value.toNSData(), key, expire.toUInt()) }
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> mmkv.getBoolForKey(key, default) as T
        is Int -> mmkv.getInt32ForKey(key, default) as T
        is Long -> mmkv.getInt64ForKey(key, default) as T
        is Float -> mmkv.getFloatForKey(key, default) as T
        is Double -> mmkv.getDoubleForKey(key, default) as T
        is String -> mmkv.getStringForKey(key, default) as T
        is ByteArray -> (mmkv.getDataForKey(key, default.toNSData())?.toByteArray() ?: default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = mmkv.containsKey(key)
    actual fun remove(key: String) { mmkv.removeValueForKey(key) }
}