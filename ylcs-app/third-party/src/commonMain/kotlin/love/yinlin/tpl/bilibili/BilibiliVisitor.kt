package love.yinlin.tpl.bilibili

import androidx.compose.runtime.Stable

@Stable
data class BilibiliVisitor(
    val imgKey: String,
    val subKey: String,
    val serverTimeOffset: Long
)