package love.yinlin.startup

import kotlinx.browser.localStorage
import kotlinx.io.files.Path
import love.yinlin.extension.*
import love.yinlin.platform.KVExpire
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupFetcher
import love.yinlin.service.SyncStartup

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
actual class StartupKV : SyncStartup {
    actual override fun init(context: PlatformContext, args: StartupArgs) {}

    private fun setItem(key: String, value: String, expire: Int) {
        val time = if (expire == KVExpire.NEVER) expire else (DateEx.CurrentLong / 1000L).toInt() + expire
        localStorage.setItem(key, makeArray {
            add(time)
            add(value)
        }.toJsonString())
    }

    fun getItem(key: String): String? = localStorage.getItem(key)?.let { catchingNull {
        val arr = it.parseJson.Array
        val time = arr[0].Int
        val value = arr[1].String
        val current = (DateEx.CurrentLong / 1000L).toInt()
        if (time == KVExpire.NEVER || current <= time) value
        else {
            localStorage.removeItem(key)
            null
        }
    } }

    actual fun set(key: String, value: Boolean, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Int, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Long, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Float, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: Double, expire: Int) = setItem(key, value.toString(), expire)
    actual fun set(key: String, value: String, expire: Int) = setItem(key, value, expire)
    actual fun set(key: String, value: ByteArray, expire: Int) = setItem(key, value.toJsonString(JsonConverter.ByteArray), expire)
    actual inline fun <reified T : Any> get(key: String, default: T): T {
        val value = getItem(key)
        return if (value == null) default else when (default) {
            is Boolean, is Int, is Long, is Float, is Double -> value.parseJsonValue<T>()!!
            is String -> value as T
            is ByteArray -> value.parseJsonValue(JsonConverter.ByteArray) as? T ?: default
            else -> default
        }
    }
    actual operator fun contains(key: String): Boolean = getItem(key) != null
    actual fun remove(key: String) { localStorage.removeItem(key) }
}