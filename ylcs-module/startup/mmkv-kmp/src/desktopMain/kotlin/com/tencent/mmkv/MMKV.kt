package com.tencent.mmkv

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class MMKV(path: String) {
    companion object {
        init {
            NativeLibLoader.resource("mmkv_kmp")
        }

        @JvmStatic
        private external fun nativeInit(path: String): Long
        @JvmStatic
        private external fun nativeSetBoolean(handle: Long, key: String, value: Boolean, expire: Int)
        @JvmStatic
        private external fun nativeSetInt(handle: Long, key: String, value: Int, expire: Int)
        @JvmStatic
        private external fun nativeSetLong(handle: Long, key: String, value: Long, expire: Int)
        @JvmStatic
        private external fun nativeSetFloat(handle: Long, key: String, value: Float, expire: Int)
        @JvmStatic
        private external fun nativeSetDouble(handle: Long, key: String, value: Double, expire: Int)
        @JvmStatic
        private external fun nativeSetString(handle: Long, key: String, value: String, expire: Int)
        @JvmStatic
        private external fun nativeSetByteArray(handle: Long, key: String, value: ByteArray, expire: Int)
        @JvmStatic
        private external fun nativeGetBoolean(handle: Long, key: String, default: Boolean): Boolean
        @JvmStatic
        private external fun nativeGetInt(handle: Long, key: String, default: Int): Int
        @JvmStatic
        private external fun nativeGetLong(handle: Long, key: String, default: Long): Long
        @JvmStatic
        private external fun nativeGetFloat(handle: Long, key: String, default: Float): Float
        @JvmStatic
        private external fun nativeGetDouble(handle: Long, key: String, default: Double): Double
        @JvmStatic
        private external fun nativeGetString(handle: Long, key: String, default: String): String
        @JvmStatic
        private external fun nativeGetByteArray(handle: Long, key: String, default: ByteArray): ByteArray
        @JvmStatic
        private external fun nativeContains(handle: Long, key: String): Boolean
        @JvmStatic
        private external fun nativeRemove(handle: Long, key: String)
    }

    val nativeHandle: Long = nativeInit(path)

    fun set(key: String, value: Boolean, expire: Int) = nativeSetBoolean(nativeHandle, key, value, expire)
    fun set(key: String, value: Int, expire: Int) = nativeSetInt(nativeHandle, key, value, expire)
    fun set(key: String, value: Long, expire: Int) = nativeSetLong(nativeHandle, key, value, expire)
    fun set(key: String, value: Float, expire: Int) = nativeSetFloat(nativeHandle, key, value, expire)
    fun set(key: String, value: Double, expire: Int) = nativeSetDouble(nativeHandle, key, value, expire)
    fun set(key: String, value: String, expire: Int) = nativeSetString(nativeHandle, key, value, expire)
    fun set(key: String, value: ByteArray, expire: Int) = nativeSetByteArray(nativeHandle, key, value, expire)
    fun getBoolean(key: String, default: Boolean) = nativeGetBoolean(nativeHandle, key, default)
    fun getInt(key: String, default: Int) = nativeGetInt(nativeHandle, key, default)
    fun getLong(key: String, default: Long) = nativeGetLong(nativeHandle, key, default)
    fun getFloat(key: String, default: Float) = nativeGetFloat(nativeHandle, key, default)
    fun getDouble(key: String, default: Double) = nativeGetDouble(nativeHandle, key, default)
    fun getString(key: String, default: String) = nativeGetString(nativeHandle, key, default)
    fun getByteArray(key: String, default: ByteArray) = nativeGetByteArray(nativeHandle, key, default)
    operator fun contains(key: String): Boolean = nativeContains(nativeHandle, key)
    fun remove(key: String) = nativeRemove(nativeHandle, key)
}