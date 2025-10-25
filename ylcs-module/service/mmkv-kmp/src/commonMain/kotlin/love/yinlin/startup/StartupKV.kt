package love.yinlin.startup

import kotlinx.io.files.Path
import love.yinlin.platform.KVExpire
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupFetcher
import love.yinlin.service.SyncStartup

@StartupFetcher(index = 0, name = "initPath", returnType = Path::class)
expect class StartupKV() : SyncStartup {
    override fun init(context: PlatformContext, args: StartupArgs)

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