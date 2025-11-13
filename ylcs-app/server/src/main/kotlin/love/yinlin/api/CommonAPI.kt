package love.yinlin.api

import love.yinlin.Local
import love.yinlin.data.rachel.server.ServerStatus

fun APIScope.apiCommon(implMap: ImplMap) {
    ApiCommonGetServerStatus.response {
        result(ServerStatus(
            targetVersion = Local.info.version,
            minVersion = Local.info.minVersion,
        ))
    }
}