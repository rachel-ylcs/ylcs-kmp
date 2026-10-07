package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 屏幕创建策略
 *
 * 1. New 直接创建新页面，并附加在导航栈顶，不共享任何资源和ViewModel。
 * 2. Replace 先在导航栈中查找原栈中最靠近栈顶的同类型条目，如果存在则将先其移除。触发New。
 * 3. Move 先在导航栈中查找原栈中最靠近栈顶的同类型条目，如果存在则将其直接移到导航栈顶，但不做任何更新操作；如果不存在则触发New。
 * 4. Resume 先在导航栈中查找原栈中最靠近栈顶的同类型条目，如果存在则将其直接移到导航栈顶，并调用页面的resume；如果不存在则触发New。
 */
@Stable
enum class CreatePolicy {
    New,
    Replace,
    Move,
    Resume;

    operator fun plus(policy: ClearPolicy): NavigationPolicy = NavigationPolicy(this, policy)
}