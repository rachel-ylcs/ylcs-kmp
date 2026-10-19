package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable

/**
 * 同一 Compose 布局根内的背景模糊状态。
 *
 * 每个状态连接一个 [blurSource]，允许多个 [blurTarget] 共享该来源。不同页面应持有独立实例。
 * 状态本身不拥有平台资源；捕获层随 Modifier 节点释放，不应保存到可恢复状态中。
 */
@Stable
class BlurState {
    internal var source: BlurSourceNode? = null
        private set

    private val targets: MutableList<BlurTargetNode> = []

    internal val hasTargets: Boolean get() = targets.isNotEmpty()

    internal fun attachSource(node: BlurSourceNode) {
        check(source == null || source === node) { "A BlurState can only have one attached blurSource." }
        source = node
        node.updateCaptureDemand()
        invalidateTargets()
    }

    internal fun detachSource(node: BlurSourceNode) {
        if (source === node) {
            source = null
            invalidateTargets()
        }
    }

    internal fun attachTarget(node: BlurTargetNode) {
        if (node !in targets) {
            targets += node
            source?.updateCaptureDemand()
        }
    }

    internal fun detachTarget(node: BlurTargetNode) {
        if (targets.remove(node)) source?.updateCaptureDemand()
    }

    internal fun invalidateTargets() {
        for (target in targets) target.invalidateSource()
    }
}
