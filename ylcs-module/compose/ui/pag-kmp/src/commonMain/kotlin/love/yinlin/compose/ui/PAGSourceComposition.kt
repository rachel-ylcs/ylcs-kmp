package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

/**
 * 包含若干个 [PAGSource] 的组合，如果不提供宽高则默认使用第一个 Source 的宽高
 */

@Stable
class PAGSourceComposition(
    internal val sources: List<PAGSource>,
    internal val width: Int? = null,
    internal val height: Int? = null,
) {
    constructor(source: PAGSource) : this(listOf(source))
}