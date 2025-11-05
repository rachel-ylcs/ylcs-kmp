package love.yinlin.startup

import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.StartupFetcher
import love.yinlin.StartupNative
import love.yinlin.SyncStartup
import love.yinlin.extension.NativeLib
import love.yinlin.platform.*

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
@StartupNative
@NativeLib
actual class StartupKV : SyncStartup {
    var nativeHandle: Long = 0

    actual override fun init(context: Context, args: StartupArgs) {
        val path: Path? = args.fetch(0)
        if (path != null) nativeHandle = nativeInit(path.toString())
    }
    actual fun set(key: String, value: Boolean, expire: Int) = nativeSetBoolean(nativeHandle, key, value, expire)
    actual fun set(key: String, value: Int, expire: Int) = nativeSetInt(nativeHandle, key, value, expire)
    actual fun set(key: String, value: Long, expire: Int) = nativeSetLong(nativeHandle, key, value, expire)
    actual fun set(key: String, value: Float, expire: Int) = nativeSetFloat(nativeHandle, key, value, expire)
    actual fun set(key: String, value: Double, expire: Int) = nativeSetDouble(nativeHandle, key, value, expire)
    actual fun set(key: String, value: String, expire: Int) = nativeSetString(nativeHandle, key, value, expire)
    actual fun set(key: String, value: ByteArray, expire: Int) = nativeSetByteArray(nativeHandle, key, value, expire)
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> nativeGetBoolean(nativeHandle, key, default) as T
        is Int -> nativeGetInt(nativeHandle, key, default) as T
        is Long -> nativeGetLong(nativeHandle, key, default) as T
        is Float -> nativeGetFloat(nativeHandle, key, default) as T
        is Double -> nativeGetDouble(nativeHandle, key, default) as T
        is String -> nativeGetString(nativeHandle, key, default) as T
        is ByteArray -> nativeGetByteArray(nativeHandle, key, default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = nativeContains(nativeHandle, key)
    actual fun remove(key: String) = nativeRemove(nativeHandle, key)
}