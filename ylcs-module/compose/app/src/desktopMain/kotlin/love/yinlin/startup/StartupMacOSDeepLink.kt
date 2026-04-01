package love.yinlin.startup

import love.yinlin.coroutines.cpuContext
import love.yinlin.uri.Uri
import love.yinlin.uri.toUri
import love.yinlin.platform.Platform
import love.yinlin.foundation.Startup
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import java.awt.Desktop
import kotlin.coroutines.CoroutineContext

class StartupMacOSDeepLink(
    pool: StartupPool,
    private val handler: (Uri) -> Unit
) : Startup(pool) {
    class Factory(private val handler: (Uri) -> Unit) : StartupFactory<StartupMacOSDeepLink> {
        override val id: String = StartupID<StartupMacOSDeepLink>()
        override val dependencies: List<String> = emptyList()
        override val dispatcher: CoroutineContext = cpuContext
        override fun build(pool: StartupPool): StartupMacOSDeepLink = StartupMacOSDeepLink(pool, handler)
    }

    override suspend fun init() {
        Platform.use(Platform.MacOS) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                handler(event.uri.toUri())
            }
        }
    }
}