package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

/**
 * 屏幕导航策略
 *
 * 此类型仅描述请求，不执行导航。具体栈变更由后续的导航计算负责。
 * 可使用 [CreatePolicy] + [ClearPolicy] 组合策略。
 */
@Stable
data class NavigationPolicy(val createPolicy: CreatePolicy, val clearPolicy: ClearPolicy) {
    companion object {
        val Default = NavigationPolicy(createPolicy = CreatePolicy.New, clearPolicy = ClearPolicy.None)
    }
}