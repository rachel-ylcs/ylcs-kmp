package love.yinlin.tpl.bilibili

import love.yinlin.compose.ui.text.*
import love.yinlin.data.bilibili.BilibiliRichNode
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue

fun bilibiliRichTextToRichString(richText: String): RichString = buildRichString {
    val items: List<BilibiliRichNode> = catchingNull { richText.parseJsonValue() } ?: [BilibiliRichNode.Text(text = richText)]

    for (item in items) {
        when (item) {
            is BilibiliRichNode.Br -> br()
            is BilibiliRichNode.Text -> text(str = item.text)
            is BilibiliRichNode.Emoji -> image(uri = item.url)
            is BilibiliRichNode.Link -> link(uri = item.url, text = item.text)
            is BilibiliRichNode.VideoLink -> link(uri = item.url, text = item.text)
            is BilibiliRichNode.Topic -> topic(uri = item.url, text = item.text)
            is BilibiliRichNode.At -> at(uri = item.id, text = item.text)
            is BilibiliRichNode.Picture -> { }
        }
    }
}