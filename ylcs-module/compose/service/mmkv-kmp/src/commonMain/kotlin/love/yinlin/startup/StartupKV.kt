package love.yinlin.startup

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.platform.KVExpire
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.StartupFetcher
import love.yinlin.SyncStartup

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
@Stable
expect class StartupKV() : SyncStartup {
    override fun init(context: Context, args: StartupArgs)

    fun set(key: String, value: Boolean, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Int, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Long, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Float, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: Double, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: String, expire: Int = KVExpire.NEVER)
    fun set(key: String, value: ByteArray, expire: Int = KVExpire.NEVER)
    inline fun <reified T : Any> get(key: String, default: T): T
    operator fun contains(key: String): Boolean
    fun remove(key: String)
}