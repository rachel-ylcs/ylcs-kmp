package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.*
import love.yinlin.compose.platform.inspector

private class SemanticsNode(
    var role: Role?,
    var contentType: ContentType?,
    var description: String?
) : Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        var status = false
        this@SemanticsNode.role?.let {
            this.role = it
            status = true
        }
        this@SemanticsNode.contentType?.let {
            this.contentType = it
            status = true
        }
        this@SemanticsNode.description?.let {
            this.contentDescription = it
            status = true
        }
        if (!status) hideFromAccessibility()
    }
}

private data class SemanticsElement(
    val role: Role?,
    val contentType: ContentType?,
    val description: String?
) : ModifierNodeElement<SemanticsNode>() {
    override fun create(): SemanticsNode = SemanticsNode(role, contentType, description)
    override fun update(node: SemanticsNode) {
        node.role = role
        node.contentType = contentType
        node.description = description
        node.invalidateSemantics()
    }
    override fun InspectorInfo.inspectableProperties() = inspector("Semantics") {
        "role" bind (role ?: "null")
        "contentType" bind (contentType ?: "null")
        "description" bind (description ?: "null")
    }
}

@Stable
fun Modifier.semantics(role: Role? = null, contentType: ContentType? = null, description: String? = null): Modifier =
    this then SemanticsElement(role, contentType, description)