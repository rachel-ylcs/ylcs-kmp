package love.yinlin.tpl.douyin

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import love.yinlin.common.TPProxy
import love.yinlin.data.douyin.Douyin
import love.yinlin.data.douyin.DouyinData
import love.yinlin.data.douyin.DouyinMedia
import love.yinlin.data.douyin.DouyinUserInfo
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader

@Stable
object DouyinAPI {
    @Stable
    private object Fetcher {
        fun extractUserInfo(user: JsonObject): DouyinUserInfo {
            // 提取名称和头像
            val userId = user["sec_uid"].String
            val uid = user["uid"].String
            val userName = user["nickname"].String
            val thumbAvatar = user.obj("avatar_thumb").arr("url_list")[0].String
            val avatar = TPProxy.proxyRes(thumbAvatar.replace("100x100", "720x720"))
            return DouyinUserInfo(
                id = userId,
                uid = uid,
                name = userName,
                avatar = avatar
            )
        }

        fun extractMedias(aweme: JsonObject): List<DouyinMedia> {
            val medias: MutableList<DouyinMedia> = []

            val pics = aweme["images"].ArrayEmpty
            if (pics.isNotEmpty()) { // 图文
                for (picItem in pics) {
                    val pic = picItem.Object
                    medias += DouyinMedia.Image(pic.arr("url_list")[0].String)
                }
            }
            else if ("video" in aweme) { // 视频
                val video = aweme["video"].Object
                val playAddr = video["play_addr"].ObjectNull ?: video["play_addr_264"].ObjectNull ?: video["play_addr_265"].Object
                val cover = video["cover"].ObjectNull ?: video["raw_cover"].ObjectNull ?: video["origin_cover"].Object
                medias += DouyinMedia.Video(
                    cover = cover.arr("url_list")[0].String,
                    video = playAddr.arr("url_list").map { it.String }.reversed() // 视频一般底部有效
                )
            }

            return medias
        }

        fun extractDouyin(aweme: JsonObject): Douyin {
            // 提取ID
            val awemeId = aweme["aweme_id"].String
            val userInfo = extractUserInfo(aweme.obj("author"))
            // 提取时间
            val time = (aweme["create_time"].Long * 1000).toLocalDateTime!!
            // 提取IP
            val location = aweme["region"]?.StringNull ?: "IP未知"
            // 提取标题和内容
            val title = (aweme["preview_title"] ?: aweme["caption"]).StringNull ?: ""
            val content = aweme["desc"].String
            // 提取数据
            val statistics = aweme.obj("statistics")
            val data = DouyinData(
                recommendNum = statistics["recommend_count"].Int,
                commentNum = statistics["comment_count"].Int,
                likeNum = statistics["digg_count"].Int,
                admireNum = statistics["admire_count"].Int,
                repostNum = statistics["share_count"].Int,
                collectNum = statistics["collect_count"].Int
            )
            return Douyin(
                id = awemeId,
                user = userInfo,
                time = time,
                location = location,
                title = title,
                content = content,
                data = data,
                medias = extractMedias(aweme)
            )
        }
    }

    // ######## 相关API ########

    private const val DEFAULT_TTWID = "1%7CvDWCB8tYdKPbdOlqwNTkDPhizBaV9i91KjYLKJbqurg%7C1723536402%7C314e63000decb79f46b8ff255560b29f4d8c57352dad465b41977db4830b4c7e"
    private const val DEFAULT_UIFID = "ca297c42bcefdf0ae322da592c17a6a6fdf368337d4fcdb6ba6eba9bdfe56c67580be2b4c6d26444fafc4eb7c2d1853bd16d73944c0a162531782cb16bcf8b7a0edaa71fa55bbe2e1c396d81744021e4"

    suspend fun generateCookie(): DouyinCookie {
        val result = NetClient.Common.request<ByteArray, Pair<String, String>>({
            url = "https://www.douyin.com/"
            method = HttpMethod.Head
        }) {
            val ttwid = cookies.first { it.name.equals("ttwid", ignoreCase = true) }.value
            val uifid = cookies.first { it.name.equals("UIFID_TEMP", ignoreCase = true) }.value
            ttwid to uifid
        }

        val fp = DouyinEncoder.buildFingerprint()
        val msToken = DouyinEncoder.buildMsToken()

        return DouyinCookie(
            ttwid = result?.first ?: DEFAULT_TTWID,
            fp = fp,
            msToken = msToken,
            uifid = result?.second ?: DEFAULT_UIFID
        )
    }

    suspend fun requestUserDouyin(id: String, cookie: DouyinCookie): List<Douyin>? = NetClient.Common.request({
        url = DouyinEncoder.buildUrl(id, cookie)
        headers = [
            HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
            HttpHeaders.Referrer to "${DouyinUrl.BASE_URL}/"
        ]
        cookies = cookie.asCookies
    }) { json: JsonObject ->
        val awemeList = json.arr("aweme_list")
        val items: MutableList<Douyin> = []
        for (item in awemeList) items += Fetcher.extractDouyin(item.Object)
        items
    }
}