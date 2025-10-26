package love.yinlin.startup

import love.yinlin.platform.MusicFactory
import love.yinlin.platform.buildMusicFactory
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArgs
import love.yinlin.service.SyncStartup

class StartupMusicFactory : SyncStartup {
    lateinit var instance: MusicFactory

    override fun init(context: PlatformContext, args: StartupArgs) {
        instance = buildMusicFactory(context)
        instance.initFactory()
    }
}