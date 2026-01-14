package love.yinlin.cs

import love.yinlin.extension.toJsonString

class NineGridProcessor(pics: APIFile?) {
    val sourcePics: APIFile = pics ?: EmptyAPIFile
    val actualPics: List<String> = if (sourcePics.files.size > 9) throw error("NineGrid invalid num") else List(sourcePics.files.size) { currentUniqueId(it) }
    val jsonString: String = actualPics.toJsonString()

    inline fun copy(callback: (String) -> APIFile): String? {
        actualPics.forEachIndexed { index, name ->
            sourcePics[index].copy(callback(name))
        }
        return actualPics.firstOrNull()
    }
}