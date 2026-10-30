package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 屏幕清理策略
 * 1. 无操作。
 * 2. 清理沿途的所有其他页面。
 * 3. 切换前移除当前页面，但 Pop 本身不移除最后一页；同类当前页仅按创建策略处理。
 */
@Stable
enum class ClearPolicy {
    None,
    Clear,
    Pop;

    operator fun plus(policy: CreatePolicy): NavigationPolicy = NavigationPolicy(policy, this)
}