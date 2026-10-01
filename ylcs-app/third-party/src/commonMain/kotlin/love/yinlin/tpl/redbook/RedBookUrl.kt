package love.yinlin.tpl.redbook

import androidx.compose.runtime.Stable

@Stable
object RedBookUrl {
    fun userProfile(id: String): String = "https://www.xiaohongshu.com/user/profile/$id"
}