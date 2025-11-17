package love.yinlin

import io.ktor.server.application.PipelineCall
import io.ktor.server.request.uri
import love.yinlin.api.*
import love.yinlin.api.user.AN
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry
import love.yinlin.extension.catchingError
import love.yinlin.server.Redis

val APIScope.callMap by lazy { buildCallBackMap<Mail.Filter, MailEntry, String>() }

fun main() = object : ServerEngine() {
    override val port: Int = 1211

    override val isCopyResources: Boolean = true

    override val loggerClass = MainLogger::class

    override val configFile: String = "config.json"

    override val plugins: List<ServerPlugin> = listOf(
        object : ServerPlugin("IPMonitor") {
            override fun onCall(call: PipelineCall) {
                catchingError {
                    val request = call.request
                    val ip = request.headers["X-Real-IP"]
                    val uri = request.uri
                    if (uri.startsWith("/user") || uri.startsWith("/sys") || uri.startsWith("/res"))
                        logger.debug("URL: {} IP: {}", uri, ip)
                }?.let { err ->
                    logger.error("RequestListener - {}", err.stackTraceToString())
                }
            }
        }
    )

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

    override val useDatabase: Boolean = true
    override val useRedis: Boolean = true

    override fun Redis.onRedisCreate() {
        AN.initAESKey(this)
    }

    override fun onStart() {
        logger.info("服务器启动")
    }
}.run()