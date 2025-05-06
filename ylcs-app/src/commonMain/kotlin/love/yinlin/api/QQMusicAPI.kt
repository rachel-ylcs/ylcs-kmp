package love.yinlin.api

import love.yinlin.common.Uri
import love.yinlin.data.Data
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.JsonObjectScope
import love.yinlin.extension.Object
import love.yinlin.extension.makeObject
import love.yinlin.extension.obj
import love.yinlin.extension.parseJson
import love.yinlin.extension.toJsonString
import love.yinlin.platform.app
import love.yinlin.platform.safeGet

object QQMusicAPI {
    private inline fun buildUrl(url: String, data: JsonObjectScope.() -> Unit): String = "$url?data=${Uri.encodeUri(makeObject(data).toJsonString())}"

    private fun decodeData(body: ByteArray) = body.decodeToString().parseJson.Object.obj("req_0").obj("data")

    suspend fun requestMusicId(url: String): Data<String> = app.client.safeGet(url) { body: ByteArray ->
        "\"mid\":\\s*\"([^\"]*)".toRegex().find(body.decodeToString())!!.groupValues[1]
    }

    suspend fun requestMusic(id: String): Data<PlatformMusicInfo> {
        app.client.safeGet(buildUrl("https://u.y.qq.com/cgi-bin/musicu.fcg") {
            obj("req_0") {
                "module" with "vkey.GetVkeyServer"
                "method" with "CgiGetVkey"
                obj("param") {
                    "guid" with "10000"
                    "uin" to "0"
                    "loginflag" to 1
                    "platform" to "20"
                    arr("filename") { add("C400${id}.m4a") }
                    arr("songmid") { add(id) }
                    arr("songtype") { add(0) }
                }
            }
            obj("comm") {
                "uin" with "0"
                "format" with "json"
                "ct" with 24
                "cv" with 0
            }
        }) { body: ByteArray ->
            val json = decodeData(body)
            println(json)
        }
        return Data.Error()
    }
}