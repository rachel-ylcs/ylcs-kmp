package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 屏幕创建策略
 *
 * 1. New 直接创建新页面，并附加在导航栈顶，不共享任何资源和ViewModel。
 * 2. Replace 先在导航栈中查找最晚创建的同类对象，如果存在则将先其移除。触发New。
 * 3. Move 先在导航栈中查找最晚创建的同类对象，如果存在则将其直接移到导航栈顶，但不做任何更新操作；如果不存在则触发New。
 * 4. Resume 先在导航栈中查找最晚创建的同类对象，如果存在则将其直接移到导航栈顶，并调用页面的resume；如果不存在则触发New。
 */
@Stable
internal enum class CreatePolicy {
    New, Replace, Move, Resume;
}

/**
 * 屏幕清理策略
 * 1. 无操作。
 * 2. 清理沿途的所有其他页面。
 */
@Stable
enum class ClearPolicy {
    None, Clear;
}

/**
 * 屏幕导航策略
 */
@Stable
class NavigationPolicy private constructor(
    internal val createPolicy: CreatePolicy,
    internal val clearPolicy: ClearPolicy,
) {
    operator fun plus(policy: ClearPolicy): NavigationPolicy = NavigationPolicy(createPolicy, policy)

    internal operator fun component1() = createPolicy
    internal operator fun component2() = clearPolicy

    companion object {
        val New = NavigationPolicy(CreatePolicy.New, ClearPolicy.None)
        val Replace = NavigationPolicy(CreatePolicy.Replace, ClearPolicy.None)
        val Move = NavigationPolicy(CreatePolicy.Move, ClearPolicy.None)
        val Resume: NavigationPolicy = NavigationPolicy(CreatePolicy.Resume, ClearPolicy.None)
    }
}