package love.yinlin.cs

import love.yinlin.Local
import love.yinlin.data.rachel.server.ServerStatus

fun ServerScope.commonAPI() {
    ApiCommonGetServerStatus.response {
        result(ServerStatus(
            targetVersion = Local.info.version,
            minVersion = Local.info.minVersion,
        ))
    }
}