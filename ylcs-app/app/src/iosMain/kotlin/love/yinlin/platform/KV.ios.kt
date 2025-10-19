package love.yinlin.platform

import androidx.compose.runtime.Stable
import cocoapods.MMKV.MMKV
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.extension.toByteArray
import love.yinlin.extension.toNSData

@Stable
@OptIn(ExperimentalForeignApi::class)
actual class KV {
    // MMKV initialized in RachelApp.swift
    val kv = MMKV.defaultMMKV()!!

    actual fun set(key: String, value: Boolean, expire: Int) {
        kv.setBool(value, key, expire.toUInt())
    }

    actual fun set(key: String, value: Int, expire: Int) {
        kv.setInt32(value, key, expire.toUInt())
    }

    actual fun set(key: String, value: Long, expire: Int) {
        kv.setInt64(value, key, expire.toUInt())
    }

    actual fun set(key: String, value: Float, expire: Int) {
        kv.setFloat(value, key, expire.toUInt())
    }

    actual fun set(key: String, value: Double, expire: Int) {
        kv.setDouble(value, key, expire.toUInt())
    }

    actual fun set(key: String, value: String, expire: Int) {
        kv.setString(value, key, expire.toUInt())
    }

    actual fun set(key: String, value: ByteArray, expire: Int) {
        kv.setData(value.toNSData(), key, expire.toUInt())
    }

    actual inline fun <reified T> get(key: String, default: T): T {
        return when (default) {
            is Boolean -> kv.getBoolForKey(key, default) as T
            is Int -> kv.getInt32ForKey(key, default) as T
            is Long -> kv.getInt64ForKey(key, default) as T
            is Float -> kv.getFloatForKey(key, default) as T
            is Double -> kv.getDoubleForKey(key, default) as T
            is String -> kv.getStringForKey(key, default) as T
            is ByteArray -> (kv.getDataForKey(key, default.toNSData())?.toByteArray() ?: default) as T
            else -> default
        }
    }

    actual fun has(key: String): Boolean {
        return kv.containsKey(key)
    }

    actual fun remove(key: String) {
        kv.removeValueForKey(key)
    }
}