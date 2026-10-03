package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.arr
import love.yinlin.extension.obj

@Stable
@Serializable
sealed interface BilibiliRichNode {
    @Stable
    @Serializable
    data object Br : BilibiliRichNode

    @Stable
    @Serializable
    data class Text(val text: String) : BilibiliRichNode

    @Stable
    @Serializable
    data class Emoji(val id: String, val text: String, val url: String) : BilibiliRichNode

    @Stable
    @Serializable
    data class Link(val text: String, val url: String, val id: String?) : BilibiliRichNode

    @Stable
    @Serializable
    data class VideoLink(val text: String, val url: String, val id: String) : BilibiliRichNode

    @Stable
    @Serializable
    data class At(val text: String, val id: String) : BilibiliRichNode

    @Stable
    @Serializable
    data class Topic(val text: String, val url: String) : BilibiliRichNode

    @Stable
    @Serializable
    data class Picture(val id: String, val urlList: List<String>) : BilibiliRichNode

    companion object {
        fun parse(nodes: JsonArray): Pair<List<BilibiliRichNode>, List<Picture>> {
            val pictures: MutableList<Picture> = []
            val items = buildList {
                for (node in nodes) {
                    val item = node.Object
                    when (item["type"].String) {
                        "RICH_TEXT_NODE_TYPE_TEXT" -> { // 文本
                            val text = item["text"].String
                            add(if (text.isBlank()) Br else Text(text = text))
                        }
                        "RICH_TEXT_NODE_TYPE_EMOJI" -> { // 表情
                            val emoji = item.obj("emoji")
                            add(Emoji(
                                id = emoji["id"].String,
                                text = emoji["text"].String,
                                url = emoji["icon_url"].String
                            ))
                        }
                        "RICH_TEXT_NODE_TYPE_GOODS" -> { // 商品链接
                            add(Link(text = item["text"].String, url = item["jump_url"].String, id = item["rid"].String))
                        }
                        "RICH_TEXT_NODE_TYPE_BV" -> { // 视频链接
                            add(VideoLink(text = item["text"].String, url = item["jump_url"].String, id = item["rid"].String))
                        }
                        "RICH_TEXT_NODE_TYPE_WEB" -> { // 网页链接
                            add(Link(text = item["text"].String, url = item["jump_url"].String, id = null))
                        }
                        "RICH_TEXT_NODE_TYPE_AT" -> { // AT
                            add(At(text = item["text"].String, id = item["rid"].String))
                        }
                        "RICH_TEXT_NODE_TYPE_TOPIC" -> { // 话题
                            add(Topic(text = item["text"].String, url = "https://${item["jump_url"].String}"))
                        }
                        "RICH_TEXT_NODE_TYPE_VIEW_PICTURE" -> { // 图片
                            val pics = item.arr("pics")
                            add(Picture(id = item["rid"].String, urlList = pics.map { it.Object["src"].String }))
                        }
                        else -> { } // 未知富文本节点
                    }
                }
            }
            return items to pictures
        }
    }
}