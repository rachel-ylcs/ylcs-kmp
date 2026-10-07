package love.yinlin.compose.screen

import androidx.compose.runtime.Stable

@Stable
internal object NavigationCalculator {
    private fun entryIds(backStack: List<ScreenKey>): Set<ScreenID> {
        val ids: MutableSet<ScreenID> = []
        for (key in backStack) {
            require(ids.add(key.id)) { "The navigation stack cannot contain duplicate ScreenID." }
        }
        return ids
    }

    private fun stackChange(backStack: List<ScreenKey>, targetIndex: Int, clearPolicy: ClearPolicy): NavigationPlan.StackChange {
        if (targetIndex == -1) return NavigationPlan.StackChange(backStack.size, backStack.size)

        val toIndex = if (clearPolicy == ClearPolicy.Clear) backStack.size else targetIndex + 1
        return NavigationPlan.StackChange(targetIndex, toIndex)
    }

    /**
     * 按类型匹配原栈中最靠近栈顶的条目，忽略其参数。
     *
     * New 以及未匹配目标时忽略 Clear。Move、Resume 保留原 key；
     * Resume 的新参数放在计算结果中，不覆盖 key 的初始参数。
     * 计划只记录索引和目标引用，不创建新栈列表，不复制或序列化已有参数。
     * 索引以当前栈为准，调用方需保证计算与执行之间原栈不变，并串行执行。
     */
    fun navigate(
        backStack: List<ScreenKey>,
        type: String,
        args: ScreenArgs = ScreenArgs.Empty,
        policy: NavigationPolicy = NavigationPolicy.Default,
        isUnboundKey: Boolean = false
    ): NavigationPlan {
        require(type.isNotBlank())

        val ids = entryIds(backStack)
        val targetIndex = if (policy.createPolicy == CreatePolicy.New) -1 else backStack.indexOfLast { it.type == type && it.isUnboundKey == isUnboundKey }

        if (targetIndex == -1 || policy.createPolicy == CreatePolicy.Replace) {
            val key = ScreenKey(type, args, isUnboundKey = isUnboundKey)
            require(key.id !in ids) { "The ScreenID of the new entry is duplicated with the original stack." }
            return NavigationPlan.Create(key, stackChange(backStack, targetIndex, policy.clearPolicy))
        }

        val key = backStack[targetIndex]
        val change = if (targetIndex == backStack.lastIndex) null else stackChange(backStack, targetIndex, policy.clearPolicy)

        return if (policy.createPolicy == CreatePolicy.Resume) NavigationPlan.Resume(key, change, args) else NavigationPlan.Reuse(key, change)
    }
}
