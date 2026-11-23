package love.yinlin.lint.rule

import com.intellij.lang.ASTNode
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.BODY
import io.github.ktlint.core.rule.engine.core.api.ElementType.CLASS
import io.github.ktlint.core.rule.engine.core.api.ElementType.CLASS_BODY
import io.github.ktlint.core.rule.engine.core.api.ElementType.ENUM_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.ENUM_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.FOR
import io.github.ktlint.core.rule.engine.core.api.ElementType.IF
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC
import io.github.ktlint.core.rule.engine.core.api.ElementType.OBJECT_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.SEMICOLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.THEN
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHILE
import io.github.ktlint.core.rule.engine.core.api.*
import org.jetbrains.kotlin.lexer.KtTokens

internal class NoSemicolonRule : RuleV2(RuleId(ID), About(maintainer = "YLCS")) {
    override fun beforeVisitChildNodes(node: ASTNode) {
        if (node.elementType != SEMICOLON) return

        val nextLeaf = node.nextLeaf

        if (node.isEnumSeparator()) {
            if (!nextLeaf.isWhiteSpace) {
                emit(node.startOffset + 1, "Missing spacing after \";\"")
            }
            return
        }

        if (nextLeaf != null && !nextLeaf.isWhiteSpace) {
            if (!node.prevLeaf.isWhiteSpaceWithNewline) {
                emit(node.startOffset + 1, "Missing spacing after \";\"")
            }
            return
        }

        if (nextLeaf.doesNotRequirePreSemi() && node.isNoSemicolonRequiredAfter()) {
            emit(node.startOffset, "Unnecessary semicolon")
        }
    }

    private fun ASTNode.isEnumSeparator(): Boolean {
        val body = parent ?: return false

        if (body.elementType == ENUM_ENTRY) {
            return body.nextCodeSibling?.elementType != ENUM_ENTRY
        }

        if (body.elementType != CLASS_BODY) return false
        val enumClass = body.parent ?: return false
        if (enumClass.elementType != CLASS || !enumClass.hasModifier(ENUM_KEYWORD)) return false

        val previousEntry = prevCodeLeaf?.findParentByType(ENUM_ENTRY)
        if (previousEntry?.findParentByType(CLASS_BODY) == body) {
            return nextCodeSibling?.elementType != ENUM_ENTRY
        }

        return body.firstChildNode.nextCodeSibling == this && body.lastChildLeafOrSelf.prevCodeLeaf != this
    }

    private fun ASTNode.isNoSemicolonRequiredAfter(): Boolean {
        val previousCodeLeaf = prevCodeLeaf
        if (previousCodeLeaf?.elementType == OBJECT_KEYWORD) return false

        val previousParent = previousCodeLeaf?.parent ?: return true
        return !previousParent.isLoopWithoutBody() && !previousParent.isIfExpressionWithoutThen()
    }

    private fun ASTNode.isLoopWithoutBody() = (elementType == WHILE || elementType == FOR) && findChildByType(BODY)?.firstChildNode == null

    private fun ASTNode.isIfExpressionWithoutThen() = elementType == IF && findChildByType(THEN)?.firstChildNode == null

    private fun ASTNode?.doesNotRequirePreSemi(): Boolean =
        when {
            this == null -> true
            isWhiteSpace -> {
                nextLeaf { it.isCode && it.findParentByType(KDOC) == null && it.findParentByType(ANNOTATION_ENTRY) == null }.let { nextCodeLeaf ->
                    nextCodeLeaf == null || (textContains('\n') && nextCodeLeaf.elementType != KtTokens.LBRACE)
                }
            }
            else -> false
        }

    companion object {
        const val ID = "ylcs:no-semi"
    }
}
