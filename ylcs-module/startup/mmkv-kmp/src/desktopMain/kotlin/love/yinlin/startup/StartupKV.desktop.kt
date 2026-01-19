package love.yinlin.startup

import com.tencent.mmkv.MMKV
import kotlinx.io.files.Path
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupFetcher
import love.yinlin.foundation.StartupNative
import love.yinlin.foundation.SyncStartup

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
@StartupNative
@NativeLibApi
actual class StartupKV : SyncStartup() {
    lateinit var kv: MMKV

    actual override fun init(context: Context, args: StartupArgs) {
        kv = MMKV(args.fetch<Path>(0).toString())
    }
    actual fun set(key: String, value: Boolean, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Int, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Long, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Float, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: Double, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: String, expire: Int) = kv.set(key, value, expire)
    actual fun set(key: String, value: ByteArray, expire: Int) = kv.set(key, value, expire)
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> kv.getBoolean(key, default) as T
        is Int -> kv.getInt(key, default) as T
        is Long -> kv.getLong(key, default) as T
        is Float -> kv.getFloat(key, default) as T
        is Double -> kv.getDouble(key, default) as T
        is String -> kv.getString(key, default) as T
        is ByteArray -> kv.getByteArray(key, default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = kv.contains(key)
    actual fun remove(key: String) = kv.remove(key)
}