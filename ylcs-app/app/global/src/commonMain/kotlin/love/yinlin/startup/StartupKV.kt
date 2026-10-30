package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory
import love.yinlin.fs.File
import love.yinlin.kv.KVManager

class StartupKV(pool: StartupPool, initPath: File) : SyncStartup(pool) {
    class Factory(private val initPath: File) : SyncStartupFactory<StartupKV>() {
        override val id: String = StartupID<StartupKV>()
        override fun build(pool: StartupPool) = StartupKV(pool, initPath)
    }

    init {
        KVManager.initialize(pool.rawContext, initPath)
    }

    val kv get() = KVManager.kv

    override fun destroy() = KVManager.shutdown()
}
