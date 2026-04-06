package love.yinlin.startup

import love.yinlin.uri.Uri
import love.yinlin.uri.toUri
import love.yinlin.platform.Platform
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory
import java.awt.Desktop

class StartupMacOSDeepLink(pool: StartupPool, handler: (Uri) -> Unit) : SyncStartup(pool) {
    class Factory(private val handler: (Uri) -> Unit) : SyncStartupFactory<StartupMacOSDeepLink>() {
        override val id: String = StartupID<StartupMacOSDeepLink>()
        override fun build(pool: StartupPool): StartupMacOSDeepLink = StartupMacOSDeepLink(pool, handler)
    }

    init {
        Platform.use(Platform.MacOS) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                handler(event.uri.toUri())
            }
        }
    }
}