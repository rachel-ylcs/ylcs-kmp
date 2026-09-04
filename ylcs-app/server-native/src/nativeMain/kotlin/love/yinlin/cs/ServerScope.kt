package love.yinlin.cs

import love.yinlin.cs.service.RedisService

class ServerScope(engine: ServerEngine) : APIScope(engine) {
    val redis: RedisService = RedisService(this)

    override val services: List<ServerService> = listOf(
        redis
    )

    override fun api() {
        commonAPI()
    }
}