package love.yinlin.startup

import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import kotlinx.coroutines.CoroutineScope
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup
import love.yinlin.fs.File

@StartupArg(index = 0, name = "initPath", type = File::class)
actual class StartupKV actual constructor(context: PlatformContextProvider): SyncStartup(context) {
    val mmkv: MMKV = run {
        MMKV.initialize(context.raw, MMKVLogLevel.LevelNone)
        MMKV.defaultMMKV()
    }

    actual override fun init(scope: CoroutineScope, args: StartupArgs) { }

    actual fun set(key: String, value: Boolean, expire: Int) { mmkv.encode(key, value, expire) }
    actual fun set(key: String, value: Int, expire: Int) { mmkv.encode(key, value, expire) }
    actual fun set(key: String, value: Long, expire: Int) { mmkv.encode(key, value, expire) }
    actual fun set(key: String, value: Float, expire: Int) { mmkv.encode(key, value, expire) }
    actual fun set(key: String, value: Double, expire: Int) { mmkv.encode(key, value, expire) }
    actual fun set(key: String, value: String, expire: Int) { mmkv.encode(key, value, expire) }
    actual fun set(key: String, value: ByteArray, expire: Int) { mmkv.encode(key, value, expire) }
    actual inline fun <reified T : Any> get(key: String, default: T): T = when (default) {
        is Boolean -> mmkv.decodeBool(key, default) as T
        is Int -> mmkv.decodeInt(key, default) as T
        is Long -> mmkv.decodeLong(key, default) as T
        is Float -> mmkv.decodeFloat(key, default) as T
        is Double -> mmkv.decodeDouble(key, default) as T
        is String -> mmkv.decodeString(key, default) as T
        is ByteArray -> mmkv.decodeBytes(key, default) as T
        else -> default
    }
    actual operator fun contains(key: String): Boolean = mmkv.containsKey(key)
    actual fun remove(key: String) = mmkv.removeValueForKey(key)
}