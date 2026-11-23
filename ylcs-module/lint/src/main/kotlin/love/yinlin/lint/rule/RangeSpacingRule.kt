package love.yinlin.lint.rule

import com.intellij.lang.ASTNode
import io.github.ktlint.core.rule.engine.core.api.ElementType.RANGE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RANGE_UNTIL
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.prevLeaf

internal class RangeSpacingRule : RuleV2(RuleId(ID), About(maintainer = "YLCS")) {
    override fun beforeVisitChildNodes(node: ASTNode) {
        if (node.elementType != RANGE && node.elementType != RANGE_UNTIL) return
        if (!node.prevLeaf.hasSpacing() || !node.nextLeaf.hasSpacing()) {
            emit(node.startOffset, "范围运算符 ${node.text} 两侧必须留空格")
        }
    }

    private fun ASTNode?.hasSpacing(): Boolean = this != null && isWhiteSpace && (textContains(' ') || textContains('\n'))

    companion object {
        const val ID = "ylcs:range-spacing"
    }
}
