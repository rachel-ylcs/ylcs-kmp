package love.yinlin.tpl.radio

import androidx.compose.runtime.Stable

@Stable
object RadioUrl {
    const val API_BASE = "https://music.163.com/api"
    private const val MAX_LIMIT = 100

    fun user(id: String): String = "$API_BASE/djradio/v2/get?id=$id"
    fun programs(id: String): String = "$API_BASE/dj/program/byradio?radioId=$id&limit=$MAX_LIMIT&offset=0&asc=false"
    fun audioUrl(id: String): String = "$API_BASE/song/enhance/player/url?ids=[$id]&br=999000"
}