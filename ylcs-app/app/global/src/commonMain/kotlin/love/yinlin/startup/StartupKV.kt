package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory
import love.yinlin.fs.File
import love.yinlin.foundation.kv.KV

class StartupKV(pool: StartupPool, initPath: File) : SyncStartup(pool) {
    class Factory(private val initPath: File) : SyncStartupFactory<StartupKV>() {
        override val id: String = StartupID<StartupKV>()
        override fun build(pool: StartupPool) = StartupKV(pool, initPath)
    }

    val instance = KV(pool.rawContext, initPath)

    override fun destroy() = instance.close()
}
