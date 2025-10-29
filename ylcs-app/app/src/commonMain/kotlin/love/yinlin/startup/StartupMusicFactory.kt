package love.yinlin.startup

import love.yinlin.platform.MusicFactory
import love.yinlin.platform.buildMusicFactory
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup

class StartupMusicFactory : SyncStartup {
    lateinit var instance: MusicFactory

    override fun init(context: Context, args: StartupArgs) {
        instance = buildMusicFactory(context)
        instance.initFactory()
    }
}