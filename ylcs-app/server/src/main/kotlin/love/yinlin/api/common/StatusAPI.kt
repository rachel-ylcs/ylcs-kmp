package love.yinlin.api.common

import io.ktor.server.routing.Routing
import love.yinlin.Local
import love.yinlin.api.API2
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.data.Data
import love.yinlin.data.rachel.server.ServerStatus

fun Routing.statusAPI(implMap: ImplMap) {
    api(API2.Common.Status.GetServerStatus) { ->
        Data.Success(ServerStatus(
            targetVersion = Local.info.version,
            minVersion = Local.info.minVersion,
        ))
    }
}