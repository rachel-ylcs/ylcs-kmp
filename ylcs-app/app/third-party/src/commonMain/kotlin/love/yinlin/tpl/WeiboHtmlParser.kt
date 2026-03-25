package love.yinlin.tpl

import love.yinlin.compose.ui.text.*
import love.yinlin.tpl.WeiboAPI.Container

private sealed interface Node
private data class TextNode(val text: String) : Node
private data object BrNode : Node
private data class ImgNode(val src: String) : Node
private data class SpanNode(val children: List<Node>) : Node
private data class ANode(val href: String, val children: List<Node>) : Node

/**
 * 递归下降解析微博主题HTML
 */
private fun parseWeiboHtml(html: String): List<Node>? {
    var pos = 0
    val length = html.length

    fun skipWhitespace() {
        while (pos < length && html[pos].isWhitespace()) pos++
    }

    fun parseNodes(endTagName: String?): List<Node>? {
        val nodes = mutableListOf<Node>()

        while (pos < length) {
            val nextOpen = html.indexOf('<', pos)

            if (nextOpen == -1) {
                val textContent = html.substring(pos).trim()
                if (textContent.isNotEmpty()) nodes += TextNode(textContent)
                pos = length
                break
            }

            if (nextOpen > pos) {
                val textContent = html.substring(pos, nextOpen).trim()
                if (textContent.isNotEmpty()) nodes += TextNode(textContent)
            }

            pos = nextOpen + 1
            if (pos >= length) break

            if (html[pos] == '/') {
                pos++
                val closeEnd = html.indexOf('>', pos)
                if (closeEnd == -1) return null

                val tagName = html.substring(pos, closeEnd).trim().lowercase()
                pos = closeEnd + 1

                return if (tagName == endTagName) nodes else null
            } else {
                val tagNameStart = pos
                while (pos < length && !html[pos].isWhitespace() && html[pos] != '>' && html[pos] != '/') pos++
                val tagName = html.substring(tagNameStart, pos).lowercase()

                var isSelfClosing = false
                var href = ""
                var src = ""

                while (pos < length) {
                    skipWhitespace()
                    if (pos >= length) break

                    val c = html[pos]
                    if (c == '/') {
                        isSelfClosing = true
                        pos++
                    } else if (c == '>') {
                        pos++
                        break
                    } else {
                        val attrStart = pos
                        while (pos < length && !html[pos].isWhitespace() && html[pos] != '=' && html[pos] != '>' && html[pos] != '/') pos++
                        val attrName = html.substring(attrStart, pos).lowercase()

                        skipWhitespace()
                        var attrValue = ""
                        if (pos < length && html[pos] == '=') {
                            pos++
                            skipWhitespace()
                            if (pos < length) {
                                val quote = html[pos]
                                if (quote == '"' || quote == '\'') {
                                    pos++
                                    val valStart = pos
                                    val valEnd = html.indexOf(quote, pos)
                                    if (valEnd == -1) return null
                                    attrValue = html.substring(valStart, valEnd).trim()
                                    pos = valEnd + 1
                                } else {
                                    val valStart = pos
                                    while (pos < length && !html[pos].isWhitespace() && html[pos] != '>' && html[pos] != '/') pos++
                                    attrValue = html.substring(valStart, pos).trim()
                                }
                            }
                        }

                        if (tagName == "a" && attrName == "href") href = attrValue
                        else if (tagName == "img" && attrName == "src") src = attrValue
                    }
                }

                when (tagName) {
                    "br" -> nodes += BrNode
                    "img" if src.isNotEmpty() -> nodes += ImgNode(src)
                    "span" if !isSelfClosing -> nodes += SpanNode(parseNodes("span") ?: return null)
                    "a" if !isSelfClosing && href.isNotEmpty() -> nodes += ANode(href, parseNodes("a") ?: return null)
                }
            }
        }

        return if (endTagName != null) null else nodes
    }

    return parseNodes(null)
}

fun weiboHtmlToRichString(html: String): RichString = buildRichString {
    fun weiboHtmlToRichList(nodes: List<Node>, list: RichList) {
        for (node in nodes) {
            when (node) {
                is TextNode -> {
                    val text = node.text
                    if (text.startsWith('#') && text.endsWith('#')) { // # 话题
                        val topic = text.removePrefix("#").removeSuffix("#")
                        list.topic(Container.searchTopic(topic), text)
                    }
                    else list.text(text) // 普通文本
                }
                is BrNode -> list.br()
                is ImgNode -> list.image(node.src)
                is SpanNode -> weiboHtmlToRichList(node.children, list)
                is ANode -> {
                    val href = node.href
                    val children = node.children
                    val text = (children.firstOrNull() as? TextNode)?.text
                    if (children.size == 1 && text != null) {
                        if (href.startsWith("/n/")) list.at(Container.href(href), text) // @ 标签
                        else if (href.startsWith("/status/")) list.link(Container.href(href), text)
                        else list.link(href, text)
                    }
                    else weiboHtmlToRichList(children, list)
                }
            }
        }
    }

    weiboHtmlToRichList(parseWeiboHtml(html) ?: return@buildRichString, this)
}