package love.yinlin.cs

import love.yinlin.cs.service.MysqlService
import love.yinlin.cs.service.RedisService
import love.yinlin.cs.user.Authorization
import love.yinlin.cs.user.Verification

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

    @Suppress("PropertyName")
    val VN = Verification(logger, mysql)

    @Suppress("PropertyName")
    val AN = Authorization(logger, redis)
}