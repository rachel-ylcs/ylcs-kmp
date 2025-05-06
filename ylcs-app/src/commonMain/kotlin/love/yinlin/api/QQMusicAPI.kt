package love.yinlin.api

import kotlinx.serialization.json.JsonObject
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
    private inline fun buildUrl(data: JsonObjectScope.() -> Unit): String =
        "https://u.y.qq.com/cgi-bin/musicu.fcg?data=${Uri.encodeUri(makeObject(data).toJsonString())}"

    private fun decodeData(num: Int, body: ByteArray): List<JsonObject> {
        val json = body.decodeToString().parseJson.Object
        val arr = mutableListOf<JsonObject>()
        repeat(num) {
            arr += json.obj("req_$it").obj("data")
        }
        return arr
    }

    suspend fun requestMusicId(url: String): Data<String> = app.client.safeGet(url) { body: ByteArray ->
        "\"mid\":\\s*\"([^\"]*)".toRegex().find(body.decodeToString())!!.groupValues[1]
    }

    suspend fun requestMusic(id: String): Data<PlatformMusicInfo> {
        app.client.safeGet(buildUrl {
            obj("req_0") {
                "module" with "music.pf_song_detail_svr"
                "method" with "get_song_detail_yqq"
                obj("param") { "song_mid" with id }
            }
            obj("req_1") {
                "module" with "music.musichallSong.PlayLyricInfo"
                "method" with "GetPlayLyricInfo"
                obj("param") {
                    "songMID" with id
                }
            }
            obj("req_2") {
                "module" with "vkey.GetVkeyServer"
                "method" with "CgiGetVkey"
                obj("param") {
                    arr("filename") { add("C400$id$id.m4a") }
                    arr("songmid") { add(id) }
                    arr("songtype") { add(0) }
                    "guid" to "10000"
                }
            }
        }) { body: ByteArray ->
            val json = decodeData(3, body)
            println(json)
        }
        return Data.Error()
    }
}