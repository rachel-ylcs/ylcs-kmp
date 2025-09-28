package love.yinlin.api

import love.yinlin.extension.toJsonString
import love.yinlin.server.copy
import love.yinlin.server.currentUniqueId

class NineGridProcessor(val sourcePics: APIFiles) {
    val actualPics: List<String> = if (sourcePics.size > 9) throw error("NineGrid invalid num") else List(sourcePics.size) { currentUniqueId(it) }
    val jsonString: String = actualPics.toJsonString()

    inline fun copy(callback: (String) -> ResNode): String? {
        repeat(actualPics.size) {
            sourcePics[it].copy(callback(actualPics[it]))
        }
        return actualPics.firstOrNull()
    }
}