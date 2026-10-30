package love.yinlin.foundation.kv

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class NativeKV(path: String) : AutoCloseable {
    companion object {
        init {
            NativeLibLoader.resource("ylcs-module.foundation.kv")
        }

        @JvmStatic
        private external fun nativeInit(path: String): Long
        @JvmStatic
        private external fun nativeClose(handle: Long)
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

    private var nativeHandle: Long = nativeInit(path).also { check(it != 0L) { "Cannot open KV storage" } }

    private inline fun <T> withHandle(block: (Long) -> T): T {
        check(nativeHandle != 0L) { "KV is closed" }
        return block(nativeHandle)
    }

    override fun close() {
        val previous = nativeHandle
        nativeHandle = 0L
        if (previous != 0L) nativeClose(previous)
    }

    fun set(key: String, value: Boolean, expire: Int) = withHandle { nativeSetBoolean(it, key, value, expire) }
    fun set(key: String, value: Int, expire: Int) = withHandle { nativeSetInt(it, key, value, expire) }
    fun set(key: String, value: Long, expire: Int) = withHandle { nativeSetLong(it, key, value, expire) }
    fun set(key: String, value: Float, expire: Int) = withHandle { nativeSetFloat(it, key, value, expire) }
    fun set(key: String, value: Double, expire: Int) = withHandle { nativeSetDouble(it, key, value, expire) }
    fun set(key: String, value: String, expire: Int) = withHandle { nativeSetString(it, key, value, expire) }
    fun set(key: String, value: ByteArray, expire: Int) = withHandle { nativeSetByteArray(it, key, value, expire) }
    fun getBoolean(key: String, default: Boolean) = withHandle { nativeGetBoolean(it, key, default) }
    fun getInt(key: String, default: Int) = withHandle { nativeGetInt(it, key, default) }
    fun getLong(key: String, default: Long) = withHandle { nativeGetLong(it, key, default) }
    fun getFloat(key: String, default: Float) = withHandle { nativeGetFloat(it, key, default) }
    fun getDouble(key: String, default: Double) = withHandle { nativeGetDouble(it, key, default) }
    fun getString(key: String, default: String) = withHandle { nativeGetString(it, key, default) }
    fun getByteArray(key: String, default: ByteArray) = withHandle { nativeGetByteArray(it, key, default) }
    operator fun contains(key: String): Boolean = withHandle { nativeContains(it, key) }
    fun remove(key: String) = withHandle { nativeRemove(it, key) }
}
