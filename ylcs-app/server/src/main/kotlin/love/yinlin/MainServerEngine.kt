package love.yinlin

import love.yinlin.api.*
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry

@Suppress("unused", "unchecked_cast")
data object MainServerEngine : ServerEngine() {
    override val public: String = ServerRes.toString()
    override val proxy: Proxy = Proxy(name = APIConfig.PROXY_NAME, whitelist = listOf(
        "(?:https?://)?m\\.weibo\\.cn.*".toRegex(),
        "(?:https?://)?visitor\\.passport\\.weibo\\.cn.*".toRegex(),
        "(?:https?://)?(?:wx|tvax)\\d+\\.sinaimg\\.cn.*".toRegex(),
        "(?:https?://)?f\\.video\\.weibocdn\\.com.*".toRegex(),
    ))

    override fun scope() = APIScope<Mail.Filter, MailEntry, String>()

    override val APIScope<out Any, *, *>.api get() = (this as APIScope<Mail.Filter, MailEntry, String>).run {
        listOf(
            ::commonAPI,
            ::photoAPI,
            ::accountAPI,
            ::activityAPI,
            ::backupAPI,
            ::followsAPI,
            ::gameAPI,
            ::mailAPI,
            ::profileAPI,
            ::topicAPI,
            ::songAPI
        )
    }
}