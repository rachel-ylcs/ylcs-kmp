package love.yinlin.cs.resource

import love.yinlin.cs.APIFile
import love.yinlin.cs.EmptyAPIFile
import love.yinlin.cs.copy
import love.yinlin.cs.get
import love.yinlin.extension.DateEx
import love.yinlin.extension.toJsonString

// 九宫格图片上传
class NineGridProcessor(pics: APIFile?) {
    val sourcePics: APIFile = pics ?: EmptyAPIFile
    val actualPics: List<String> = if (sourcePics.files.size > 9) error("NineGrid invalid num") else List(sourcePics.files.size) { DateEx.uniqueTimeId(it) }
    val jsonString: String = actualPics.toJsonString()

    inline fun copy(callback: (String) -> APIFile): String? {
        actualPics.forEachIndexed { index, name ->
            sourcePics[index].copy(callback(name))
        }
        return actualPics.firstOrNull()
    }
}