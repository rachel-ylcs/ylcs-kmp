package love.yinlin.startup

import cocoapods.MMKV.MMKV
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import love.yinlin.extension.toNSData
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.StartupFetcher
import love.yinlin.SyncStartup

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
@OptIn(ExperimentalForeignApi::class)
actual class StartupKV : SyncStartup {
    // TODO: 需要重新review
    lateinit var mmkv: MMKV

    actual override fun init(context: Context, args: StartupArgs) {
        // TODO: 把MMKV.initialized从swift中移到这里
        // MMKV initialized in RachelApp.swift
        mmkv = MMKV.defaultMMKV()!!
    }

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