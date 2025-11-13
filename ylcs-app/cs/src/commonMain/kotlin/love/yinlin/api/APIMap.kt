package love.yinlin.api

import love.yinlin.data.rachel.server.ServerStatus

val ApiCommonGetServerStatus by API.post.i().o<ServerStatus>()