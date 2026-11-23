package love.yinlin.foundation.kv

import kotlinx.browser.localStorage
import love.yinlin.extension.*
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File

actual class KV actual constructor(context: PlatformContext, initPath: File) : AutoCloseable {
    private val keyPrefix = initPath.path.trimStart('/').let { "kv:${it.length}:$it:" }

    actual override fun close() { }

    private fun storageKey(key: String): String = keyPrefix + key

    private fun setItem(key: String, value: String, expire: Int) {
        val time = if (expire == KVExpire.NEVER) expire else (DateEx.CurrentLong / 1000L).toInt() + expire
        localStorage.setItem(storageKey(key), makeArray {
            add(time)
            add(value)
        }.toJsonString())
    }

    fun getItem(key: String): String? = localStorage.getItem(storageKey(key))?.let {
        catchingNull {
            val arr = it.parseJson.Array
            val time = arr[0].Int
            val value = arr[1].String
            val current = (DateEx.CurrentLong / 1000L).toInt()
            if (time == KVExpire.NEVER || current <= time) value
            else {
                remove(key)
                null
            }
        }
    }

    actual fun set(key: String, value: Boolean, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Int, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Long, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Float, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Double, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: String, expire: Int) = setItem(key, value, expire)
    actual fun set(key: String, value: ByteArray, expire: Int) = setItem(key, value.toJsonString(ByteArraySerializer), expire)
    actual inline fun <reified T : Any> get(key: String, default: T): T {
        val value = getItem(key)
        return if (value == null) default else when (default) {
            is Boolean, is Int, is Long, is Float, is Double -> value.parseJsonValue<T>()
            is String -> value as T
            is ByteArray -> value.parseJsonValue(ByteArraySerializer) as? T ?: default
            else -> default
        }
    }
    actual operator fun contains(key: String): Boolean = getItem(key) != null
    actual fun remove(key: String) { localStorage.removeItem(storageKey(key)) }
}
