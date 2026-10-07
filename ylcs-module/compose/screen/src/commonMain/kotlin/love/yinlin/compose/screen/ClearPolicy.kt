package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 屏幕清理策略
 * 1. 无操作。
 * 2. 清理沿途的所有其他页面。
 */
@Stable
enum class ClearPolicy {
    None,
    Clear;

    operator fun plus(policy: CreatePolicy): NavigationPolicy = NavigationPolicy(policy, this)
}