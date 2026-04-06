package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartupFactory
import love.yinlin.fs.File

class StartupKVFactory(private val initPath: File) : SyncStartupFactory<StartupKV>() {
    override val id: String = StartupID<StartupKV>()
    override fun build(pool: StartupPool): StartupKV = StartupKV(pool, initPath)
}