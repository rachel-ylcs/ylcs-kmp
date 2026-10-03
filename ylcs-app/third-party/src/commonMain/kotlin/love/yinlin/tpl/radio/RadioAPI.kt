package love.yinlin.tpl.radio

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.radio.*
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient

@Stable
object RadioAPI {
    @Stable
    private object Fetcher {
        fun extractUserInfo(json: JsonObject): RadioUserInfo = RadioUserInfo(
            id = json["userId"].String,
            name = json["nickname"].String,
            avatar = json["avatarUrl"].String,
            signature = json["signature"].String,
        )

        fun extractUser(json: JsonObject): RadioUser {
            val data = json.obj("data")

            return RadioUser(
                id = data["id"].String,
                user = extractUserInfo(data.obj("dj")),
                name = data["name"].String,
                cover = data["picUrl"].String,
                title = data["rcmdText"].String,
                description = data["desc"].String,
                subscriberNum = data["subCount"].Int,
                shareNum = data["shareCount"].Int,
                programNum = data["programCount"].Int,
                commentNum = data["commentCount"].Int,
                updateTime = data["lastProgramCreateTime"].Long.toLocalDateTime!!
            )
        }

        fun extractProgram(json: JsonObject): Radio {
            val mainSong = json.obj("mainSong")

            return Radio(
                id = json["id"].String,
                user = extractUserInfo(json.obj("dj")),
                time = json["createTime"].Long.toLocalDateTime!!,
                location = "",
                title = json["name"].String,
                content = json["description"].String,
                data = RadioData(
                    commentNum = json["commentCount"].Int,
                    likeNum = json["likedCount"].Int,
                    repostNum = json["shareCount"].Int,
                    playNum = json["listenerCount"].Int,
                ),
                medias = [RadioMedia(image = json["coverUrl"].String)],
                duration = json["duration"].Long,
                audioId = mainSong["id"].String,
            )
        }

        fun extractPrograms(json: JsonObject): List<Radio> {
            val programs: MutableList<Radio> = []
            for (item in json.arr("programs")) {
                val program = catchingNull { extractProgram(item.Object) }
                if (program != null) programs += program
            }
            return programs
        }

        fun extractAudio(json: JsonObject): RadioAudio {
            val data = json.arr("data")[0].Object

            return RadioAudio(
                id = data["id"].String,
                url = data["url"].String,
                br = data["br"].Long,
                size = data["size"].Long,
                type = data["type"].String
            )
        }
    }

    suspend fun generateCookie(): RadioCookie? = NetClient.Common.request<ByteArray, RadioCookie>({
        url = RadioUrl.API_BASE
    }) {
        RadioCookie(nmtid = cookies["NMTID"]!!.value)
    }

    suspend fun requestUser(id: String): RadioUser? = NetClient.Common.request({
        url = RadioUrl.user(id)
    }) { json: JsonObject ->
        Fetcher.extractUser(json)
    }

    suspend fun requestPrograms(id: String, cookie: RadioCookie): List<Radio>? = NetClient.Common.request({
        url = RadioUrl.programs(id)
        cookies = cookie.asCookies
    }) { json: JsonObject ->
        Fetcher.extractPrograms(json)
    }

    suspend fun requestAudio(id: String, cookie: RadioCookie): RadioAudio? = NetClient.Common.request({
        url = RadioUrl.audioUrl(id)
        cookies = cookie.asCookies
    }) { json: JsonObject ->
        Fetcher.extractAudio(json)
    }
}