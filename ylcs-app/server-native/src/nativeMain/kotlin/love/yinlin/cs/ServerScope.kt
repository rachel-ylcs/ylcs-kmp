package love.yinlin.cs

import love.yinlin.cs.service.MysqlService
import love.yinlin.cs.service.RedisService

class ServerScope(engine: ServerEngine) : APIScope(engine) {
    val mysql: MysqlService = MysqlService(this)
    val redis: RedisService = RedisService(this)

    override val services: List<ServerService> = listOf(
        mysql,
        redis
    )

    override fun api() {
        commonAPI()
    }
}