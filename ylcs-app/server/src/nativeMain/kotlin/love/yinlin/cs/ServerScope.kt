package love.yinlin.cs

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import love.yinlin.cs.service.MysqlService
import love.yinlin.cs.service.RedisService
import love.yinlin.cs.user.Authorization
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry

class ServerScope(engine: ServerEngine) : APIScope(engine) {
    val callMap = buildCallBackMap<Mail.Filter, MailEntry, String, QueryExecutor>()
    val mysql: MysqlService = MysqlService(this)
    val redis: RedisService = RedisService(this)

    override val services: List<ServerService> = [mysql, redis]

    val checker = ServerChecker()

    override fun api() {
        accountAPI()
        activityAPI()
        backupAPI()
        commonAPI()
        concertAPI()
        followsAPI()
        gameAPI()
        mailAPI()
        photoAPI()
        profileAPI()
        rhymeAPI()
        songAPI()
        topicAPI()
    }

    @Suppress("PropertyName")
    val AN = Authorization(logger, redis)
}
