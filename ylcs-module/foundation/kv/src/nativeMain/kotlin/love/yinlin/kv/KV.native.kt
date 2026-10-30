@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.kv

import kotlinx.cinterop.*
import love.yinlin.concurrent.Lock
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.kv.internal.*
import platform.posix.size_tVar

actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    private val lock = Lock()
    private var handle: COpaquePointer? = run {
        initPath.mkdirSync()
        checkNotNull(rachel_kv_open(initPath.path)) { "Cannot open KV storage" }
    }

    private inline fun <T> withHandle(block: (COpaquePointer) -> T): T = lock.synchronized {
        block(checkNotNull(handle) { "KV is closed" })
    }

    private inline fun write(expire: Int, block: (COpaquePointer) -> Boolean) {
        require(expire >= 0)
        check(withHandle(block)) { "KV could not write the value" }
    }

    actual fun set(key: String, value: Boolean, expire: Int) = write(expire) { rachel_kv_set_bool(it, key, value, expire.toUInt()) }
    actual fun set(key: String, value: Int, expire: Int) = write(expire) { rachel_kv_set_int(it, key, value, expire.toUInt()) }
    actual fun set(key: String, value: Long, expire: Int) = write(expire) { rachel_kv_set_long(it, key, value, expire.toUInt()) }
    actual fun set(key: String, value: Float, expire: Int) = write(expire) { rachel_kv_set_float(it, key, value, expire.toUInt()) }
    actual fun set(key: String, value: Double, expire: Int) = write(expire) { rachel_kv_set_double(it, key, value, expire.toUInt()) }
    actual fun set(key: String, value: String, expire: Int) = setData(key, value.encodeToByteArray(), expire, true)
    actual fun set(key: String, value: ByteArray, expire: Int) = setData(key, value, expire, false)

    private fun setData(key: String, value: ByteArray, expire: Int, stringValue: Boolean) {
        write(expire) { pointer ->
            value.usePinned {
                val bytes = if (value.isEmpty()) null else it.addressOf(0).reinterpret<UByteVar>()
                rachel_kv_set_data(pointer, key, bytes, value.size.toULong(), expire.toUInt(), stringValue)
            }
        }
    }

    private fun getData(pointer: COpaquePointer, key: String, stringValue: Boolean): ByteArray? = memScoped {
        val size = alloc<size_tVar>()
        val bytes = rachel_kv_get_data(pointer, key, size.ptr, stringValue) ?: return@memScoped null
        try {
            check(size.value <= Int.MAX_VALUE.toULong()) { "KV value is too large" }
            bytes.reinterpret<ByteVar>().readBytes(size.value.toInt())
        }
        finally {
            rachel_kv_free(bytes)
        }
    }

    @PublishedApi
    internal fun read(key: String, default: Any): Any = withHandle { pointer ->
        when (default) {
            is Boolean -> rachel_kv_get_bool(pointer, key, default)
            is Int -> rachel_kv_get_int(pointer, key, default)
            is Long -> rachel_kv_get_long(pointer, key, default)
            is Float -> rachel_kv_get_float(pointer, key, default)
            is Double -> rachel_kv_get_double(pointer, key, default)
            is String -> getData(pointer, key, true)?.decodeKVString() ?: default
            is ByteArray -> getData(pointer, key, false) ?: default
            else -> default
        }
    }

    actual inline fun <reified T : Any> get(key: String, default: T): T = read(key, default) as T

    actual operator fun contains(key: String): Boolean = withHandle { rachel_kv_contains(it, key) }

    actual fun remove(key: String) = withHandle { rachel_kv_remove(it, key) }

    actual override fun close() {
        lock.synchronized {
            val previous = handle
            handle = null
            if (previous != null) rachel_kv_close(previous)
        }
    }
}
