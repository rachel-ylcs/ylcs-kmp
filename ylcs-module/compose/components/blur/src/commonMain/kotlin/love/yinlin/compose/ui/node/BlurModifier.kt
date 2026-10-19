package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import love.yinlin.compose.platform.inspector

/**
 * 标记 [state] 的背景来源，保留内容原本的绘制。
 *
 * 无 target 时不捕获。每个状态仅允许一个附着的 source；source 与 target 必须位于同一布局根。
 * 不要把 target 放入其自身 source 的内容中。
 */
@Stable
fun Modifier.blurSource(state: BlurState): Modifier = this then BlurSourceElement(state)

/**
 * 在当前内容下方绘制 [state] 来源的模糊背景，前景内容保持清晰。
 *
 * 按 [parameters] 填充底色、采样来源，依次应用模糊、Softlight 噪点和 SrcOver tint。
 * 来源暂时离开布局时保留最后有效画面，直到节点释放或捕获尺寸失效。
 *
 * Android 26–30 和软件 Canvas 使用随半径增强的 tint 遮罩；Android 31+ 的硬件 Canvas、
 * Desktop、iOS、JS、Wasm 使用平台模糊。边缘固定为有界 Clamp，圆角由外层 clip 决定。
 */
@Stable
fun Modifier.blurTarget(state: BlurState, parameters: BlurParameters = BlurParameters()): Modifier = this then BlurTargetElement(state, parameters)

@Stable
private data class BlurSourceElement(val state: BlurState) : ModifierNodeElement<BlurSourceNode>() {
    override fun create() = BlurSourceNode(state)

    override fun update(node: BlurSourceNode) = node.update(state)

    override fun InspectorInfo.inspectableProperties() = inspector("blurSource") {
        "state" bind state
    }
}

@Stable
private data class BlurTargetElement(val state: BlurState, val parameters: BlurParameters) : ModifierNodeElement<BlurTargetNode>() {
    override fun create() = BlurTargetNode(state, parameters)

    override fun update(node: BlurTargetNode) = node.update(state, parameters)

    override fun InspectorInfo.inspectableProperties() = inspector("blurTarget") {
        "state" bind state
        "parameters" bind parameters
    }
}
