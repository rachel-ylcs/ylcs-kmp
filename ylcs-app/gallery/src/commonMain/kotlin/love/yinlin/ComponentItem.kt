package love.yinlin

import androidx.compose.runtime.Stable

@Stable
internal data class ComponentItem(
    val title: String,
    val description: String,
    val keywords: String,
    val previewAlias: String? = null
)
