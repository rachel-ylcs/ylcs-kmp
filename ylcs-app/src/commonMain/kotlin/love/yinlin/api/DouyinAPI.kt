package love.yinlin.api

import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.douyin.DouyinVideo
import love.yinlin.extension.DateEx
import love.yinlin.extension.IntNull
import love.yinlin.extension.Long
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.arr
import love.yinlin.extension.obj
import love.yinlin.extension.toLocalDateTime

object DouyinAPI {
    private fun getDouyinVideo(json: JsonObject): DouyinVideo {
        val time = Instant.fromEpochSeconds(json["create_time"].Long).toLocalDateTime!!
        val video = json.obj("video")
        val cover = video.obj("cover").arr("url_list")
        val picUrl = cover.last().String
        val playAddr = video.obj("play_addr").arr("url_list")
        val statistics = json.obj("statistics")
        require(playAddr.isNotEmpty())
        return DouyinVideo(
            id = json["aweme_id"].String,
            title = json["desc"].String,
            createTime = DateEx.Formatter.standardDateTime.format(time)!!,
            picUrl = picUrl,
            videoUrl = playAddr.map { it.String },
            likeNum = statistics["digg_count"].IntNull ?: 0,
            commentNum = statistics["comment_count"].IntNull ?: 0,
            collectNum = statistics["collect_count"].IntNull ?: 0,
            shareNum = statistics["share_count"].IntNull ?: 0,
            isTop = (json["is_top"].IntNull ?: 0) == 1,
        )
    }

    fun getDouyinVideos(json: JsonObject): List<DouyinVideo> {
        val result = mutableListOf<DouyinVideo>()
        for (item in json.arr("aweme_list")) result += getDouyinVideo(item.Object)
        return result
    }
}