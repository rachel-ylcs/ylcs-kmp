package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.api.ImplMap
import love.yinlin.api.api

fun Routing.friendAPI(implMap: ImplMap) {
    api()
}