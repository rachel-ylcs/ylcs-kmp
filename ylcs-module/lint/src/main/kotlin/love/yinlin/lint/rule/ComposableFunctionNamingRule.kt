package love.yinlin.lint.rule

import com.intellij.lang.ASTNode
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUN
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class ComposableFunctionNamingRule : RuleV2(RuleId(ID), About(maintainer = "YLCS")) {
    override fun beforeVisitChildNodes(node: ASTNode) {
        if (node.elementType != FUN) return
        val function = node.psi as? KtNamedFunction ?: return
        if (function.annotationEntries.none { it.shortName?.asString() == "Composable" }) return

        val name = function.nameIdentifier ?: return
        val first = function.name?.firstOrNull() ?: return
        val returnsUnit = when (function.typeReference?.text) {
            "Unit", "kotlin.Unit" -> true
            null -> if (function.hasBlockBody()) true else return
            else -> false
        }

        val valid = if (returnsUnit) first.isUpperCase() else first.isLowerCase()
        if (!valid) {
            val expected = if (returnsUnit) "大写" else "小写"
            emit(name.startOffset, "Composable 函数名必须以${expected}字母开头")
        }
    }

    companion object {
        const val ID = "ylcs:composable-function-naming"
    }
}
