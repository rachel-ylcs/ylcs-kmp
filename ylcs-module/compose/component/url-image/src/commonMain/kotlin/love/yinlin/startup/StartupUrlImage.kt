package love.yinlin.startup

import com.github.panpf.sketch.SingletonSketch
import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

class StartupUrlImage : SyncStartup {
    override fun init(context: PlatformContext, args: Array<Any?>) {
        SingletonSketch.setSafe { buildSketch(context) }
    }
}

