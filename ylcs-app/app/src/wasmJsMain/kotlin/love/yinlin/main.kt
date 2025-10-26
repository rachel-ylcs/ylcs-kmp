package love.yinlin

import love.yinlin.compose.composeApplication
import love.yinlin.service.PlatformContext

fun main() {
    service.init(PlatformContext)

    composeApplication(
        entry = { framework -> AppEntry { framework() } }
    ) {
        ScreenEntry()
    }
}