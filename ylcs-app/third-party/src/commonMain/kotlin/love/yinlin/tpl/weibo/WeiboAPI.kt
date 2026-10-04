package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.common.TPProxy
import love.yinlin.data.weibo.*
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader
import love.yinlin.uri.Uri

@Stable
object WeiboAPI {
    // ######## 提取 ########
    @Stable
    private object Fetcher {
        fun extractUserInfo(user: JsonObject): WeiboUserInfo {
            // 提取名称和头像
            val userId = user["id"].String
            val userName = user["screen_name"].String
            val avatar = TPProxy.proxyRes(user["avatar_hd"].String)
            return WeiboUserInfo(
                id = userId,
                name = userName,
                avatar = avatar
            )
        }

        private fun extractHeader(blog: JsonObject): Weibo {
            // 提取ID
            val blogId = blog["id"].String
            val userInfo = extractUserInfo(blog.obj("user"))
            // 提取时间
            val time = WeiboDate.convert(blog["created_at"].String)
            // 提取IP
            val location = blog["region_name"].StringNull?.let {
                val index = it.indexOf(' ')
                if (index != -1) it.substring(index + 1) else it
            } ?: "IP未知"
            // 提取内容
            val content = blog["text"].String
            // 提取数据
            val data = WeiboData(
                commentNum = blog["comments_count"].Int,
                likeNum = blog["attitudes_count"].Int,
                repostNum = blog["reposts_count"].Int
            )
            return Weibo(
                id = blogId,
                user = userInfo,
                time = time,
                location = location,
                content = content,
                data = data,
                medias = []
            )
        }

        private fun extractWeiboMedias(rawBlog: JsonObject): List<WeiboMedia> {
            val blog = rawBlog["retweeted_status"]?.Object ?: rawBlog // 转发微博

            val medias: MutableList<WeiboMedia> = []
            val pics = blog["pics"].ArrayEmpty
            if (pics.isNotEmpty()) { // 图片微博
                for (picItem in pics) {
                    val pic = picItem.Object
                    val imageUrl = pic["url"].String
                    val sourceUrl = pic["large"].ObjectNull?.get("url")?.StringNull ?: imageUrl
                    medias += WeiboMedia.Image(
                        image = TPProxy.proxyRes(imageUrl),
                        source = TPProxy.proxyRes(sourceUrl)
                    )
                }
            }
            else if ("page_info" in blog) { // 视频微博
                val pageInfo = blog.obj("page_info")
                if (pageInfo["type"].String == "video") {
                    val urls = pageInfo.obj("urls")
                    val videoUrl = urls["mp4_720p_mp4"].StringNull ?: urls["mp4_hd_mp4"].StringNull ?: urls["mp4_ld_mp4"].String
                    val videoPicUrl = pageInfo.obj("page_pic")["url"].String
                    medias += WeiboMedia.Video(
                        image = TPProxy.proxyRes(videoPicUrl),
                        video = TPProxy.proxyRes(videoUrl)
                    )
                }
            }
            return medias
        }

        private fun extractChaohuaMedias(blog: JsonObject): List<WeiboMedia> {
            val medias: MutableList<WeiboMedia> = []
            val pics = blog["pic_infos"].ObjectEmpty
            if (pics.isNotEmpty()) { // 图片微博
                for ([_, picItem] in pics) {
                    val pic = picItem.Object
                    val imageUrl = pic[when {
                        "large" in pic -> "large"
                        "bmiddle" in pic -> "bmiddle"
                        else -> "thumbnail"
                    }].Object["url"].String
                    val sourceUrl = pic[when {
                        "original" in pic -> "original"
                        "largest" in pic -> "largest"
                        "mw2000" in pic -> "mw2000"
                        "large" in pic -> "large"
                        "bmiddle" in pic -> "bmiddle"
                        else -> "thumbnail"
                    }].Object["url"].String
                    medias += WeiboMedia.Image(
                        image = TPProxy.proxyRes(imageUrl),
                        source = TPProxy.proxyRes(sourceUrl)
                    )
                }
            }
            else if ("page_info" in blog) { // 视频微博
                val pageInfo = blog.obj("page_info")
                if (pageInfo["object_type"].String == "video") {
                    val mediaInfo = pageInfo.obj("media_info")
                    val videoUrl = mediaInfo["mp4_720p_mp4"].StringNull ?: mediaInfo["mp4_hd_url"].StringNull ?: mediaInfo["mp4_sd_url"].String
                    val videoPicUrl = pageInfo["url"].String
                    medias += WeiboMedia.Video(
                        image = TPProxy.proxyRes(videoPicUrl),
                        video = TPProxy.proxyRes(videoUrl)
                    )
                }
            }
            return medias
        }

        fun extractWeibo(blog: JsonObject): Weibo = extractHeader(blog).copy(medias = extractWeiboMedias(blog))

        fun extractChaohua(blog: JsonObject): Weibo = extractHeader(blog).copy(medias = extractChaohuaMedias(blog))

        fun extractComment(card: JsonObject): WeiboComment {
            val commentId = card["id"].String
            // 提取名称和头像
            val userInfo = extractUserInfo(card.obj("user"))
            // 提取时间
            val time = WeiboDate.convert(card["created_at"].String)
            // 提取IP
            val location = card["source"]?.StringNull?.removePrefix("来自") ?: "IP未知"
            // 提取内容
            val content = card["text"].String
            // 带图片
            val picture = if ("pic" in card) {
                val pic = card.obj("pic")
                WeiboMedia.Image(
                    image = TPProxy.proxyRes(pic["url"].String),
                    source = TPProxy.proxyRes(pic.obj("large")["url"].String)
                )
            } else null
            // 楼中楼
            val subComments: MutableList<WeiboSubComment> = []
            val comments = card["comments"]
            if (comments as? JsonArray != null) {
                for (subCard in comments) {
                    val subCardObj = subCard.Object
                    subComments += WeiboSubComment(
                        id = subCardObj["id"].String,
                        user = extractUserInfo(subCardObj.obj("user")),
                        time = WeiboDate.convert(subCardObj["created_at"].String),
                        location = subCardObj["source"]?.StringNull?.removePrefix("来自") ?: "IP未知",
                        content = subCardObj["text"].String
                    )
                }
            }
            return WeiboComment(
                id = commentId,
                user = userInfo,
                time = time,
                location = location,
                content = content,
                picture = picture,
                subComments = subComments
            )
        }
    }

    private suspend inline fun <reified R : Any> weiboRequest(
        url: String,
        cookie: WeiboCookie,
        header: NetHeader = [HttpHeaders.Referrer to "https://m.weibo.cn"],
        crossinline onRequest: () -> Unit = {},
        crossinline onResponse: suspend (JsonObject) -> R
    ): R? = NetClient.Common.request({
        this.url = url
        this.headers = header
        this.cookies = cookie.asCookies
        onRequest()
    }, onResponse)

    // ######## 相关API ########

    private const val DEFAULT_XSRF_TOKEN = "fu*you"
    private const val DEFAULT_SUB = "_2AkMeSKrwf8NxqwJRmvwUymjlZIh3zw_EieKoFFsrJRM3HRl-yT9yqhAgtRB6NciEEb-f-w8Zld8pGpTn4blqg02DqNuH"
    private const val DEFAULT_SUBP = "0033WrSXqPxfM72-Ws9jqgMF55529P9D9WhjLXMq867aPUPiUkd8wq4Y"

    // 生成cookie
    suspend fun generateCookie(): WeiboCookie {
        val xsrfToken = NetClient.Common.request<ByteArray, String>({
            url = WeiboUrl.xsrfConfig
        }) {
            cookies["XSRF-TOKEN"]!!.value
        } ?: DEFAULT_XSRF_TOKEN

        val [sub, subp] = NetClient.Common.request({
            url = WeiboUrl.genvisitor2
            method = HttpMethod.Post
            form = mapOf("cb" to "visitor_gray_callback")
        }) { text: String ->
            val json = text.substringAfter("(").substringBeforeLast(")").parseJson.Object
            val data = json.obj("data")
            data["sub"].String to data["subp"].String
        } ?: (DEFAULT_SUB to DEFAULT_SUBP)

        return WeiboCookie(sub, subp, xsrfToken)
    }

    /**
     * 获取用户详细信息
     *
     * @param uid 用户ID
     */
    suspend fun requestUser(uid: String, cookie: WeiboCookie): WeiboUser? = weiboRequest(WeiboUrl.userInfo(uid), cookie) { json: JsonObject ->
        val userInfo = json.obj("data").obj("userInfo")
        val id = userInfo["id"].String
        val name = userInfo["screen_name"].String
        val avatar = TPProxy.proxyRes(userInfo["avatar_hd"].String)
        val background = TPProxy.proxyRes(userInfo["cover_image_phone"].String)
        val signature = userInfo["description"].String
        val followNum = userInfo["follow_count"].String
        val fansNum = userInfo["followers_count_str"]?.StringNull ?: userInfo["followers_count"].String
        WeiboUser(
            user = WeiboUserInfo(id, name, avatar),
            background = background,
            signature = signature,
            followNum = followNum,
            fansNum = fansNum
        )
    }

    /**
     * 获取用户相册
     *
     * @param uid 用户ID
     */
    suspend fun requestUserAlbum(uid: String, cookie: WeiboCookie): List<WeiboAlbum>? = weiboRequest(WeiboUrl.userAlbum(uid), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items: MutableList<WeiboAlbum> = []
        for (item1 in cards) {
            val card = item1.Object
            if (card["itemid"].String.endsWith("albumeach")) {
                for (item2 in card.arr("card_group")) {
                    val album = item2.Object
                    if (album["card_type"].Int == 8) {
                        val containerId = Uri.parse(album["scheme"].String)!!.params["containerid"]!!
                        items += WeiboAlbum(
                            containerId = containerId,
                            title = album["title_sub"].String,
                            num = album["desc1"].String,
                            time = album["desc2"].String,
                            pic = TPProxy.proxyRes(album["pic"].String)
                        )
                    }
                }
            }
        }
        items
    }

    /**
     * 获取相册图片
     *
     * @param page 第一页是1
     * @param limit 一般是24
     */
    suspend fun requestUserAlbumPics(containerId: String, page: Int, limit: Int, cookie: WeiboCookie): WeiboAlbumPics? = weiboRequest(WeiboUrl.albumPics(containerId, page, limit), cookie) { json: JsonObject ->
        val data = json.obj("data")
        val cards = data.arr("cards")
        val pics: MutableList<WeiboMedia.Image> = []
        for (item1 in cards) {
            val card = item1.Object
            for (item2 in card.arr("pics")) {
                val pic = item2.Object
                pics += WeiboMedia.Image(
                    image = TPProxy.proxyRes(pic["pic_middle"].String),
                    source = TPProxy.proxyRes(pic["pic_ori"].String)
                )
            }
        }
        WeiboAlbumPics(
            items = pics.take(limit),
            count = data["count"].Int
        )
    }

    /**
     * 获取用户微博
     *
     * @param uid 用户ID
     */
    suspend fun requestUserWeibo(uid: String, cookie: WeiboCookie): List<Weibo>? = weiboRequest(WeiboUrl.userDetails(uid), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items: MutableList<Weibo> = []
        for (item in cards) {
            val card = item.Object
            if (card["card_type"].Int != 9) continue  // 非微博类型
            items += Fetcher.extractWeibo(card.obj("mblog"))
        }
        items
    }

    /**
     * 获取微博详情
     *
     * 比信息流的图片更完整
     */
    suspend fun requestWeiboDetails(id: String, cookie: WeiboCookie): Weibo? = weiboRequest(WeiboUrl.weiboDetails(id), cookie) { json: JsonObject ->
        val data = json.obj("data")
        Fetcher.extractWeibo(data)
    }

    /**
     * 获取微博评论
     *
     * @param id 微博ID
     */
    suspend fun requestWeiboComment(id: String, cookie: WeiboCookie): List<WeiboComment>? = weiboRequest(WeiboUrl.weiboComments(id), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("data")
        val items: MutableList<WeiboComment> = []
        for (item in cards) items += Fetcher.extractComment(item.Object)
        items
    }

    /**
     * 搜索微博用户
     *
     * @param key 关键词
     */
    suspend fun searchUser(key: String, cookie: WeiboCookie): List<WeiboUserInfo>? = weiboRequest(WeiboUrl.searchUser(key), cookie) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items: MutableList<WeiboUserInfo> = []
        for (item1 in cards) {
            val group = item1.Object
            if (group["card_type"].Int == 11) {
                for (item2 in group.arr("card_group")) {
                    val card = item2.Object
                    if (card["card_type"].Int == 10) {
                        val user = card["user"].Object
                        items += WeiboUserInfo(
                            id = user["id"].String,
                            name = user["screen_name"].String,
                            avatar = TPProxy.proxyRes(user["avatar_hd"].String)
                        )
                    }
                }
            }
        }
        items
    }

    /**
     * 请求超话
     *
     * @param page 初始是1
     */
    suspend fun requestChaohua(page: Int, cookie: WeiboCookie): List<Weibo>? = weiboRequest(
        url = WeiboUrl.chaohua(page),
        cookie = cookie,
        header = [HttpHeaders.Referrer to "https://weibo.com", "X-Requested-With" to "XMLHttpRequest"]
    ) { json: JsonObject ->
        val items = json.arr("items")
        val weibos: MutableList<Weibo> = []
        for (item in items) {
            catching {
                val card = item.Object
                val category = card["category"].String
                if (category == "feed") {
                    val weibo = Fetcher.extractChaohua(card["data"].Object)
                    weibos += weibo
                }
            }
        }
        weibos
    }
}