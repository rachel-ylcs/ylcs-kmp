package love.yinlin.api.common

import io.ktor.server.routing.Routing
import love.yinlin.api.ImplMap

fun Routing.commonAPI(implMap: ImplMap) {
    photoAPI(implMap)
}