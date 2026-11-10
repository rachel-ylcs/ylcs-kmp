package love.yinlin.lint.rule

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ARROW
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.COMMA
import io.github.ktlint.core.rule.engine.core.api.ElementType.CONTEXT_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION
import io.github.ktlint.core.rule.engine.core.api.ElementType.ENUM_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.GT
import io.github.ktlint.core.rule.engine.core.api.ElementType.INDICES
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACKET
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.SEMICOLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHEN_ENTRY
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.nextCodeLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

internal class TrailingCommaRule(private val site: Site) : RuleV2(RuleId(site.id), About(maintainer = "YLCS")) {
    enum class Site(val id: String, val description: String) {
        CALL("ylcs:trailing-comma-on-call-site", "调用处不能有尾随逗号"),
        DECLARATION("ylcs:trailing-comma-on-declaration-site", "声明处不能有尾随逗号")
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision
    ) {
        if (node.elementType == COMMA && node.trailingCommaSite() == site) {
            val _ = emit(node.startOffset, site.description, false)
        }
    }

    private fun ASTNode.trailingCommaSite(): Site? {
        val next = nextCodeLeaf?.elementType ?: return null
        return when (treeParent?.elementType) {
            COLLECTION_LITERAL_EXPRESSION, INDICES -> Site.CALL.takeIf { next == RBRACKET }
            TYPE_ARGUMENT_LIST -> Site.CALL.takeIf { next == GT }
            VALUE_ARGUMENT_LIST -> Site.CALL.takeIf { next == RPAR }
            CONTEXT_PARAMETER_LIST -> Site.DECLARATION.takeIf { next == RPAR }
            DESTRUCTURING_DECLARATION -> Site.DECLARATION.takeIf { next == RPAR || next == RBRACKET }
            ENUM_ENTRY -> Site.DECLARATION.takeIf { next == RBRACE || next == SEMICOLON }
            TYPE_PARAMETER_LIST -> Site.DECLARATION.takeIf { next == GT }
            VALUE_PARAMETER_LIST -> Site.DECLARATION.takeIf { next == RPAR || next == ARROW }
            WHEN_ENTRY -> Site.DECLARATION.takeIf { next == ARROW }
            else -> null
        }
    }
}
