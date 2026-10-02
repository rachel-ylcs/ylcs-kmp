package love.yinlin.tpl.redbook

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.redbook.*
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader

@Stable
object RedBookAPI {
    @Stable
    object Fetcher {
        internal fun extractHtmlByPath(src: String, vararg path: String): String? {
            fun findValueRange(src: String, key: String): IntRange? {
                val escaped = Regex.escape(key)
                val header = Regex("""(?<![A-Za-z0-9_$])(?:"$escaped"|'$escaped'|$escaped)\s*:""")
                val m = header.find(src) ?: return null

                var i = m.range.last + 1
                while (i < src.length && src[i].isWhitespace()) i++
                if (i >= src.length) return null

                val start = i
                val first = src[i]

                // 标量值：读到分隔符为止
                if (first != '{' && first != '[') {
                    var j = i
                    while (j < src.length && src[j] !in ",}]") j++
                    return start until j
                }

                // 对象 / 数组：括号配对，跳过字符串内部
                val open = first
                val close = if (first == '{') '}' else ']'
                var depth = 0
                var inStr: Char? = null
                var esc = false
                while (i < src.length) {
                    val ch = src[i]
                    if (inStr != null) {
                        when {
                            esc -> esc = false
                            ch == '\\' -> esc = true
                            ch == inStr -> inStr = null
                        }
                    } else {
                        when (ch) {
                            '"', '\'', '`' -> inStr = ch
                            open -> depth++
                            close -> if (--depth == 0) return start..i
                        }
                    }
                    i++
                }
                return null
            }

            var scope = src
            for (key in path) {
                val range = findValueRange(scope, key) ?: return null
                scope = scope.substring(range.first, range.last + 1)
            }
            return scope
        }

        fun extractRedBook(note: JsonObject): RedBook {
            val noteCard = note["noteCard"].Object
            val user = noteCard["user"].Object
            val interactInfo = noteCard["interactInfo"].Object
            val type = noteCard["type"].String
            val cover = noteCard["cover"].Object
            val defaultUrl = cover["urlDefault"].String
            val picture = if (type == "video") {
                RedBookMedia.Video(image = defaultUrl)
            }
            else {
                RedBookMedia.Image(image = defaultUrl)
            }

            return RedBook(
                id = note["index"].String,
                user = RedBookUserInfo(
                    id = user["userId"].String,
                    name = user["nickName"].String,
                    avatar = user["avatar"].String
                ),
                time = noteCard["time"].Long.toLocalDateTime!!,
                title = noteCard["displayTitle"].String,
                data = RedBookData(
                    likeNum = interactInfo["likedCount"].String.toInt()
                ),
                medias = [picture],
                xsecToken = note["xsecToken"].String
            )
        }
    }

    suspend fun requestUserProfile(id: String): List<RedBook>? = NetClient.Common.request({
        url = RedBookUrl.userProfile(id)
        headers = [
            HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
            HttpHeaders.AcceptEncoding to NetHeader.AcceptEncodingDefault
        ]
    }) { html: String ->
        val json = html.substringAfterLast("window.__INITIAL_STATE__=").substringBeforeLast("</script>")
        val notes = Fetcher.extractHtmlByPath(json, "user", "notes").parseJson.Array
        buildList {
            for (note in notes) {
                if (note is JsonArray && note.isNotEmpty()) {
                    for (subNote in note) add(Fetcher.extractRedBook(subNote.Object))
                }
            }
        }
    }
}