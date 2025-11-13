package love.yinlin.api

import io.ktor.server.routing.Routing
import kotlinx.serialization.json.JsonObject
import love.yinlin.api.common.commonAPI
import love.yinlin.api.test.testAPI
import love.yinlin.api.user.userAPI
import love.yinlin.data.rachel.mail.MailEntry

typealias ImplFunc = suspend (MailEntry) -> JsonObject
typealias ImplMap = MutableMap<String, ImplFunc>

val implMap = mutableMapOf<String, ImplFunc>()

@Suppress("unused")
object ServerStartup {
    fun run(route: Routing) {
        with(route) {
            APIScope(this).apply {
                apiCommon(implMap)
                apiUser(implMap)
            }
            
            // TODO: 迁移升级
            commonAPI(implMap)
            userAPI(implMap)
            testAPI(implMap)
        }
    }
}