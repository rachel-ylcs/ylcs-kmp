package love.yinlin.tpl.bilibili

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import io.ktor.http.fromHttpToGmtDate
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.bilibili.*
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader

@Stable
object BilibiliAPI {
    @Stable
    object Fetcher {
        fun extractUser(json: JsonObject): BilibiliUser {
            val data = json.obj("data")
            val user = BilibiliUserInfo(
                id = data["mid"].String,
                name = data["name"].String,
                avatar = data["face"].String
            )
            return BilibiliUser(
                user = user,
                sex = data["sex"].String,
                birthday = data["birthday"].String,
                signature = data["sign"].String,
                level = data["level"].Int,
                official = data.obj("official").let { official ->
                    val type = official["type"].Int
                    val title = official["title"].String
                    val desc = official["desc"].String
                    if (type != -1 && title.isNotEmpty() && desc.isNotEmpty()) {
                        BilibiliOfficial(title, desc)
                    } else null
                },
                vipTitle = data.obj("vip").let { vip ->
                    val status = vip["status"].Int
                    val label = vip.obj("label")["text"].String
                    if (status != 0 && label.isNotEmpty()) label else null
                },
                background = "https://i0.hdslb.com/${data["top_photo"].String}",
                liveRoom = data["live_room"].ObjectNull?.let { liveRoom ->
                    BilibiliLiveRoom(
                        id = liveRoom["roomid"].String,
                        url = liveRoom["url"].String,
                        title = liveRoom["title"].String,
                        cover = liveRoom["cover"].String,
                        live = liveRoom["liveStatus"].Int != 0,
                    )
                },
                school = data["school"].ObjectNull?.get("name")?.String,
                tags = data["tags"].ArrayEmpty.map { it.String },
                charging = data.obj("elec").obj("show_info")["list"].ArrayEmpty.map {
                    val payUser = it.Object
                    BilibiliUserInfo(
                        id = payUser["pay_mid"].String,
                        name = payUser["uname"].String,
                        avatar = payUser["avatar"].String
                    )
                }
            )
        }

        // 文字
        private fun extractDynamicWord(json: JsonObject, raw: BilibiliDynamic): Bilibili {
            val opus = json.obj("major").obj("opus")
            val [richText, pictures] = BilibiliRichNode.parse(opus.obj("summary").arr("rich_text_nodes"))
            val medias = buildList {
                for (pic in opus.arr("pics")) {
                    add(BilibiliMedia.Image(image = pic.Object["url"].String))
                }
                for (picture in pictures) {
                    for (url in picture.urlList) {
                        add(BilibiliMedia.Image(image = url))
                    }
                }
            }

            return Bilibili.Draw(
                raw = raw,
                title = opus["title"].String,
                content = richText.toJsonString(),
                medias = medias
            )
        }

        // 图文
        private fun extractDynamicDraw(json: JsonObject, raw: BilibiliDynamic): Bilibili {
            val opus = json.obj("major").obj("opus")
            val [richText, pictures] = BilibiliRichNode.parse(opus.obj("summary").arr("rich_text_nodes"))
            val medias = buildList {
                for (pic in opus.arr("pics")) {
                    add(BilibiliMedia.Image(image = pic.Object["url"].String))
                }
                for (picture in pictures) {
                    for (url in picture.urlList) {
                        add(BilibiliMedia.Image(image = url))
                    }
                }
            }

            return Bilibili.Draw(
                raw = raw,
                title = opus["title"].String,
                content = richText.toJsonString(),
                medias = medias
            )
        }

        // 视频
        private fun extractDynamicAV(json: JsonObject, raw: BilibiliDynamic): Bilibili {
            val archive = json.obj("major").obj("archive")
            val stat = archive.obj("stat")

            return Bilibili.Video(
                raw = raw,
                title = archive["title"].String,
                content = archive["desc"].String,
                medias = [
                    BilibiliMedia.Video(image = archive["cover"].String, aid = archive["aid"].String, bvid = archive["bvid"].String)
                ],
                danmakuNum = stat["danmaku"].String,
                playNum = stat["play"].String,
            )
        }

        // 专栏
        private fun extractDynamicArticle(json: JsonObject, raw: BilibiliDynamic): Bilibili {
            val opus = json.obj("major").obj("opus")
            val [richText, pictures] = BilibiliRichNode.parse(opus.obj("summary").arr("rich_text_nodes"))
            val medias = buildList {
                for (pic in opus.arr("pics")) {
                    add(BilibiliMedia.Image(image = pic.Object["url"].String))
                }
                for (picture in pictures) {
                    for (url in picture.urlList) {
                        add(BilibiliMedia.Image(image = url))
                    }
                }
            }

            return Bilibili.Draw(
                raw = raw,
                title = opus["title"].String,
                content = richText.toJsonString(),
                medias = medias
            )
        }

        // 转发
        private fun extractDynamicForward(json: JsonObject, raw: BilibiliDynamic): Bilibili {
            val [richText, pictures] = BilibiliRichNode.parse(json.obj("desc").arr("rich_text_nodes"))
            val medias = buildList {
                for (picture in pictures) {
                    for (url in picture.urlList) {
                        add(BilibiliMedia.Image(image = url))
                    }
                }
            }

            return Bilibili.Video(
                raw = raw,
                title = "【转发动态】",
                content = richText.toJsonString(),
                medias = medias,
                danmakuNum = "0",
                playNum = "0"
            )
        }

        fun extractDynamic(json: JsonObject): Bilibili {
            val modules = json.obj("modules")
            val author = modules.obj("module_author")
            val stat = modules.obj("module_stat")
            val dynamic = modules.obj("module_dynamic")

            val bilibili = BilibiliDynamic(
                id = json["id_str"].String,
                user = BilibiliUserInfo(
                    id = author["mid"].String,
                    name = author["name"].String,
                    avatar = author["face"].String
                ),
                time = (author["pub_ts"].String.toLong() * 1000).toLocalDateTime!!,
                location = author["pub_location_text"].String,
                data = BilibiliData(
                    commentNum = stat.obj("comment")["count"].Int,
                    likeNum = stat.obj("like")["count"].Int,
                    repostNum = stat.obj("forward")["count"].Int
                )
            )

            // https://github.com/Mr-KingLong/bilibili-API-collect/blob/master/docs/dynamic/dynamic_enum.md
            return when (json["type"].String) {
                "DYNAMIC_TYPE_WORD" -> extractDynamicWord(dynamic, bilibili)
                "DYNAMIC_TYPE_DRAW" -> extractDynamicDraw(dynamic, bilibili)
                "DYNAMIC_TYPE_AV" -> extractDynamicAV(dynamic, bilibili)
                "DYNAMIC_TYPE_ARTICLE" -> extractDynamicArticle(dynamic, bilibili)
                "DYNAMIC_TYPE_LIVE" -> error("直播分享")
                "DYNAMIC_TYPE_LIVE_RCMD" -> error("直播")
                "DYNAMIC_TYPE_COMMON_SQUARE" -> error("装扮")
                "DYNAMIC_TYPE_UGC_SEASON" -> error("合集")
                "DYNAMIC_TYPE_COURSES_SEASON" -> error("课程")
                "DYNAMIC_TYPE_MEDIALIST" -> error("收藏")
                "DYNAMIC_TYPE_FORWARD" -> extractDynamicForward(dynamic, bilibili)
                else -> error("暂不支持的动态")
            }
        }

        fun extractUserDynamic(json: JsonObject): Pair<List<Bilibili>, String?> {
            val data = json.obj("data")
            val hasMore = data["has_more"].Boolean
            val offset = data["offset"].String
            val arr = data["items"].Array
            val items: MutableList<Bilibili> = []
            for (item in arr) {
                catchingNull { extractDynamic(item.Object) }?.let { items += it }
            }
            val actualOffset = if (hasMore && offset.isNotEmpty()) offset else null
            return items to actualOffset
        }
    }

    suspend fun generateCookie(): BilibiliCookie? = NetClient.Common.request<ByteArray, BilibiliCookie>({
        url = BilibiliUrl.LANDPAGE
        headers = [HttpHeaders.UserAgent to NetHeader.UserAgentDesktop]
    }) {
        BilibiliCookie(cookies["buvid3"]!!.value, cookies["b_nut"]!!.value)
    }

    private suspend fun generateVisitor(): BilibiliVisitor? = NetClient.Common.request<JsonObject, BilibiliVisitor>({
        url = BilibiliUrl.NAV
        headers = [HttpHeaders.UserAgent to NetHeader.UserAgentDesktop]
    }) {
        val wbi = body.obj("data").obj("wbi_img")
        val imgKey = wbi["img_url"].String.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
        val subKey = wbi["sub_url"].String.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
        val serverTimeOffset = headers[HttpHeaders.Date]!!.fromHttpToGmtDate().timestamp - DateEx.CurrentLong
        BilibiliVisitor(imgKey, subKey, serverTimeOffset)
    }

    suspend fun requestUserInfo(uid: String, cookie: BilibiliCookie): BilibiliUser? {
        val visitor = generateVisitor() ?: return null
        val params = mapOf(
            "mid" to uid,
            "token" to "",
            *BilibiliEncoder.DefaultDeviceInfo,
            *BilibiliEncoder.DefaultFingerprint,
        )

        return NetClient.Common.request({
            url = BilibiliUrl.userInfo(BilibiliEncoder.signWbi(params, visitor))
            headers = [
                HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
                HttpHeaders.Referrer to "https://space.bilibili.com/$uid",
                HttpHeaders.Origin to "https://space.bilibili.com",
                HttpHeaders.Accept to NetHeader.AcceptAll
            ]
            cookies = cookie.asCookies
        }) { json: JsonObject ->
            Fetcher.extractUser(json)
        }
    }

    suspend fun requestUserNotice(uid: String, cookie: BilibiliCookie): String? {
        val params = mapOf(
            "mid" to uid,
            *BilibiliEncoder.DefaultDeviceInfo
        )

        return NetClient.Common.request({
            url = BilibiliUrl.userNotice(BilibiliEncoder.sign(params))
            headers = [
                HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
                HttpHeaders.Referrer to "https://space.bilibili.com/$uid",
                HttpHeaders.Origin to "https://space.bilibili.com",
                HttpHeaders.Accept to NetHeader.AcceptAll
            ]
            cookies = cookie.asCookies
        }) { json: JsonObject ->
            json["data"].String
        }
    }

    suspend fun requestUserDynamic(uid: String, cookie: BilibiliCookie, offset: String? = null): Pair<List<Bilibili>, String?>? {
        val visitor = generateVisitor() ?: return null
        val params = mapOf(
            "offset" to (offset ?: ""),
            "host_mid" to uid,
            "timezone_offset" to "-480",
            "features" to BilibiliUrl.FEATURE,
            *BilibiliEncoder.DefaultDeviceInfo,
            *BilibiliEncoder.DefaultFingerprint,
        )

        return NetClient.Common.request({
            url = BilibiliUrl.userDynamic(BilibiliEncoder.signWbi(params, visitor))
            headers = [
                HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
                HttpHeaders.Referrer to "https://space.bilibili.com/$uid",
                HttpHeaders.Origin to "https://space.bilibili.com",
                HttpHeaders.Accept to NetHeader.AcceptAll
            ]
            cookies = cookie.asCookies
        }) { json: JsonObject ->
            Fetcher.extractUserDynamic(json)
        }
    }

    suspend fun requestDynamicDetails(id: String, cookie: BilibiliCookie): Bilibili? {
        val visitor = generateVisitor() ?: return null
        val params = mapOf(
            "id" to id,
            "timezone_offset" to "-480",
            "features" to BilibiliUrl.FEATURE,
            *BilibiliEncoder.DefaultDeviceInfo,
        )

        return NetClient.Common.request({
            url = BilibiliUrl.dynamicDetails(BilibiliEncoder.signWbi(params, visitor))
            headers = [
                HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
                HttpHeaders.Referrer to "https://t.bilibili.com/$id",
                HttpHeaders.Origin to "https://space.bilibili.com",
                HttpHeaders.Accept to NetHeader.AcceptAll
            ]
            cookies = cookie.asCookies
        }) { json: JsonObject ->
            Fetcher.extractDynamic(json.obj("data").obj("item"))
        }
    }
}
