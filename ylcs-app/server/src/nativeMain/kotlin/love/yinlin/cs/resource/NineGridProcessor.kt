package love.yinlin.cs.resource

import love.yinlin.cs.APIFile
import love.yinlin.cs.EmptyAPIFile
import love.yinlin.cs.FailureException
import love.yinlin.extension.DateEx
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning
import love.yinlin.extension.toJsonString
import love.yinlin.fs.File

// 九宫格图片上传
class NineGridProcessor(pics: APIFile?) {
    val sourcePics: APIFile = pics ?: EmptyAPIFile
    val actualPics: List<String> = if (sourcePics.files.size > 9) error("NineGrid invalid num") else List(sourcePics.files.size) { DateEx.uniqueTimeId(it) }
    val jsonString: String = actualPics.toJsonString()
}

// 调用方负责串行化同名文件写入，并在保存引用后提交数据库事务。
internal suspend fun <R> APIFile.copyNewPictures(destinations: List<APIFile>, save: suspend () -> R): R {
    if (destinations.isEmpty()) return save()
    val outputs = destinations.map { File(it.files.first()) }
    if (outputs.any { it.existsSync() }) throw FailureException("图片文件已存在，请重试")
    var saved = false
    return cleaning({
        outputs.forEachIndexed { index, destination ->
            if (!File(files[index]).writeToSync(destination)) throw FailureException("图片保存失败")
        }
        val result = save()
        // 事务提交在调用方完成，提交结果未知时保留文件。
        saved = true
        result
    }) {
        if (!saved) outputs.forEach { file -> catching { file.deleteSync() } }
    }
}
