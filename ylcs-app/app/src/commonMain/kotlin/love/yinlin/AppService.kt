package love.yinlin

import love.yinlin.service.Service

abstract class AppService : Service(Local.info) {

}

expect val service: AppService