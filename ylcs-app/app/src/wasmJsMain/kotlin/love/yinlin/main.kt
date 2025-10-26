package love.yinlin

import love.yinlin.compose.composeApplication
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app
import love.yinlin.service.PlatformContext

fun main() {
    service.init(PlatformContext)
    ActualAppContext().apply {
        app = this
        initialize()
    }

    composeApplication(
        entry = { framework -> AppEntry { framework() } }
    ) {
        ScreenEntry()
    }
}