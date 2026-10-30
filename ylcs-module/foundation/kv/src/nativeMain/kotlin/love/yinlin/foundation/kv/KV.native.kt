@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.foundation.kv

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.foundation.kv.internal.*

actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    private var handle: COpaquePointer? = run {
        initPath.mkdirSync()
        checkNotNull(rachel_kv_open(initPath.path)) { "Cannot open KV storage" }
    }

    private inline fun <T> withHandle(key: String, block: (COpaquePointer, CPointer<ByteVar>?, ULong) -> T): T {
        val pointer = checkNotNull(handle) { "KV is closed" }
        val bytes = key.encodeToByteArray()
        return bytes.usePinned {
            block(pointer, if (bytes.isEmpty()) null else it.addressOf(0), bytes.size.toULong())
        }
    }

    private inline fun write(key: String, expire: Int, block: (COpaquePointer, CPointer<ByteVar>?, ULong) -> Boolean) {
        require(expire >= 0)
        check(withHandle(key, block)) { "KV could not write the value" }
    }

    actual fun set(key: String, value: Boolean, expire: Int) = write(key, expire) { pointer, name, size -> rachel_kv_set_bool(pointer, name, size, value, expire.toUInt()) }
    actual fun set(key: String, value: Int, expire: Int) = write(key, expire) { pointer, name, size -> rachel_kv_set_int(pointer, name, size, value, expire.toUInt()) }
    actual fun set(key: String, value: Long, expire: Int) = write(key, expire) { pointer, name, size -> rachel_kv_set_long(pointer, name, size, value, expire.toUInt()) }
    actual fun set(key: String, value: Float, expire: Int) = write(key, expire) { pointer, name, size -> rachel_kv_set_float(pointer, name, size, value, expire.toUInt()) }
    actual fun set(key: String, value: Double, expire: Int) = write(key, expire) { pointer, name, size -> rachel_kv_set_double(pointer, name, size, value, expire.toUInt()) }
    actual fun set(key: String, value: String, expire: Int) = write(key, expire) { pointer, name, keySize ->
        val utf8 = value.encodeToByteArray()
        utf8.usePinned {
            val bytes = if (utf8.isEmpty()) null else it.addressOf(0).reinterpret<UByteVar>()
            rachel_kv_set_string(pointer, name, keySize, bytes, utf8.size.toULong(), expire.toUInt())
        }
    }

    actual fun set(key: String, value: ByteArray, expire: Int) = write(key, expire) { pointer, name, keySize ->
        value.usePinned {
            val bytes = if (value.isEmpty()) null else it.addressOf(0).reinterpret<UByteVar>()
            rachel_kv_set_bytes(pointer, name, keySize, bytes, value.size.toULong(), expire.toUInt())
        }
    }

    private fun getData(pointer: COpaquePointer, key: CPointer<ByteVar>?, keySize: ULong, stringValue: Boolean): Any? = memScoped {
        val value = alloc<RachelKVValue>()
        val found = if (stringValue) rachel_kv_get_string(pointer, key, keySize, value.ptr)
        else rachel_kv_get_bytes(pointer, key, keySize, value.ptr)
        if (!found) return@memScoped null
        try {
            check(value.size <= Int.MAX_VALUE.toULong()) { "KV value is too large" }
            val size = value.size.toInt()
            when (value.kind) {
                RACHEL_KV_UTF8.toInt() -> value.data!!.reinterpret<ByteVar>().toKString()
                else -> value.data!!.reinterpret<ByteVar>().readBytes(size)
            }
        }
        finally {
            rachel_kv_release_value(value.ptr)
        }
    }

    @PublishedApi
    internal fun read(key: String, default: Any): Any = withHandle(key) { pointer, name, size ->
        when (default) {
            is Boolean -> rachel_kv_get_bool(pointer, name, size, default)
            is Int -> rachel_kv_get_int(pointer, name, size, default)
            is Long -> rachel_kv_get_long(pointer, name, size, default)
            is Float -> rachel_kv_get_float(pointer, name, size, default)
            is Double -> rachel_kv_get_double(pointer, name, size, default)
            is String -> getData(pointer, name, size, true) ?: default
            is ByteArray -> getData(pointer, name, size, false) ?: default
            else -> default
        }
    }

    actual inline fun <reified T : Any> get(key: String, default: T): T = read(key, default) as T

    actual operator fun contains(key: String): Boolean = withHandle(key) { pointer, name, size -> rachel_kv_contains(pointer, name, size) }

    actual fun remove(key: String) = withHandle(key) { pointer, name, size -> rachel_kv_remove(pointer, name, size) }

    actual override fun close() {
        val previous = handle
        handle = null
        if (previous != null) rachel_kv_close(previous)
    }
}
