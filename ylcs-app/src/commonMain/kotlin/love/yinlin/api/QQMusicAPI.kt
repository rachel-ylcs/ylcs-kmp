package love.yinlin.api

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.common.Uri
import love.yinlin.data.Data
import love.yinlin.extension.Object
import love.yinlin.extension.makeObject
import love.yinlin.extension.obj
import love.yinlin.extension.parseJson
import love.yinlin.extension.toJsonString
import love.yinlin.platform.app
import love.yinlin.platform.safeGet

@Stable
data class QQMusic(
    val id: String, // 音乐 ID
    val name: String, // 名称
    val singer: String, // 歌手
    val time: String, // 时长
    val pic: String, // 封面
    val lyrics: String, // 歌词
    val oggUrl: String, // OGG 下载链接
)

object QQMusicAPI {
    private fun makeQQMusicUrl(data: JsonObject): String = "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data=${Uri.encodeUri(data.toJsonString())}"

    private fun buildData(module: String, method: String, params: JsonObject): JsonObject = makeObject {
        obj("req_0") {
            "module" with module
            "method" with method
            obj("param") {
                "guid" with "10000"
                "uin" to "0"
                "loginflag" to 1
                "platform" to "20"
                merge(params)
            }
        }
        "loginUin" with "0"
        obj("comm") {
            "uin" with "0"
            "format" with "json"
            "ct" with 24
            "cv" with 0
        }
    }

    private fun decodeData(body: ByteArray) = body.decodeToString().parseJson.Object.obj("req_0").obj("data")

    suspend fun requestMusicId(url: String): Data<String> = app.client.safeGet(url) { body: ByteArray ->
        "\"mid\":\\s*\"([^\"]*)".toRegex().find(body.decodeToString())!!.groupValues[1]
    }

    suspend fun requestMusic(id: String): Data<QQMusic> {
        val url = makeQQMusicUrl(buildData(
            module = "vkey.GetVkeyServer",
            method = "CgiGetVkey",
            params = makeObject {
                arr("filename") { add("C400${id}.m4a") }
                arr("songmid") { add(id) }
                arr("songtype") { add(0) }
            }
        ))
        app.client.safeGet(url) { body: ByteArray ->
            val json = decodeData(body)
        }
        return Data.Error()
    }
}