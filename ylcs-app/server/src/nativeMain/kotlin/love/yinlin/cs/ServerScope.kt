package love.yinlin.cs

import love.yinlin.cs.service.MysqlService
import love.yinlin.cs.service.RedisService
import love.yinlin.cs.user.Authorization
import love.yinlin.cs.user.Verification
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry

class ServerScope(engine: ServerEngine) : APIScope(engine) {
    val callMap = buildCallBackMap<Mail.Filter, MailEntry, String>()
    val mysql: MysqlService = MysqlService(this)
    val redis: RedisService = RedisService(this)

    override val services: List<ServerService> = listOf(
        mysql,
        redis
    )

    override fun api() {
        accountAPI()
        activityAPI()
        backupAPI()
        commonAPI()
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
    val VN = Verification(logger, mysql)

    @Suppress("PropertyName")
    val AN = Authorization(logger, redis)
}