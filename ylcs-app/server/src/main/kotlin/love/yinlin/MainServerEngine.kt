package love.yinlin

import love.yinlin.api.*
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry

val APIScope.callMap by lazy { buildCallBackMap<Mail.Filter, MailEntry, String>() }

@Suppress("unused", "unchecked_cast")
data object MainServerEngine : ServerEngine() {
    override val public = ServerRes.toString()

    override val APIScope.api get() = listOf(
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

    override val proxy = Proxy(name = APIConfig.PROXY_NAME, whitelist = listOf(
        "(?:https?://)?m\\.weibo\\.cn.*".toRegex(),
        "(?:https?://)?visitor\\.passport\\.weibo\\.cn.*".toRegex(),
        "(?:https?://)?(?:wx|tvax)\\d+\\.sinaimg\\.cn.*".toRegex(),
        "(?:https?://)?f\\.video\\.weibocdn\\.com.*".toRegex(),
    ))
}