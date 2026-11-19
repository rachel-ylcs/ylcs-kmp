package love.yinlin.lint.rule

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
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
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.findParentByType
import io.github.ktlint.core.rule.engine.core.api.hasModifier
import io.github.ktlint.core.rule.engine.core.api.isCode
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.nextCodeSibling
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevCodeLeaf
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens

internal class NoSemicolonRule : RuleV2(RuleId(ID), About(maintainer = "YLCS")) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision
    ) {
        // The rule is invoked for every AST node, so do no work until a semicolon is found.
        if (node.elementType != SEMICOLON) return

        val nextLeaf = node.nextLeaf

        // Enum-entry terminators are required. Check their following whitespace separately:
        // "NORTH; }" is valid, while "NORTH;}" must still report missing spacing.
        if (node.isEnumSeparator()) {
            if (!nextLeaf.isWhiteSpace) {
                val _ = emit(node.startOffset + 1, "Missing spacing after \";\"", false)
            }
            return
        }

        // A non-whitespace token immediately after ';' needs a separator unless the semicolon
        // itself starts a new line. This branch avoids scanning ahead through comments/whitespace.
        if (nextLeaf != null && !nextLeaf.isWhiteSpace) {
            if (!node.prevLeaf.isWhiteSpaceWithNewline) {
                val _ = emit(node.startOffset + 1, "Missing spacing after \";\"", false)
            }
            return
        }

        if (nextLeaf.doesNotRequirePreSemi() && node.isNoSemicolonRequiredAfter()) {
            val _ = emit(node.startOffset, "Unnecessary semicolon", false)
        }
    }

    private fun ASTNode.isEnumSeparator(): Boolean {
        val body = parent ?: return false

        // The common form: the semicolon is part of the final enum entry.
        if (body.elementType == ENUM_ENTRY) {
            return body.nextCodeSibling?.elementType != ENUM_ENTRY
        }

        // Also accept a semicolon on its own line after the final enum entry.
        if (body.elementType != CLASS_BODY) return false
        val enumClass = body.parent ?: return false
        if (enumClass.elementType != CLASS || !enumClass.hasModifier(ENUM_KEYWORD)) return false

        val previousEntry = prevCodeLeaf?.findParentByType(ENUM_ENTRY)
        if (previousEntry?.findParentByType(CLASS_BODY) == body) {
            return nextCodeSibling?.elementType != ENUM_ENTRY
        }

        // An empty enum may use ';' to introduce members. Do not accept a trailing ';'
        // when it is the final code token before the closing brace.
        return body.firstChildNode.nextCodeSibling == this &&
            body.lastChildLeafOrSelf?.prevCodeLeaf != this
    }

    private fun ASTNode.isNoSemicolonRequiredAfter(): Boolean {
        val previousCodeLeaf = prevCodeLeaf
        if (previousCodeLeaf?.elementType == OBJECT_KEYWORD) return false

        val previousParent = previousCodeLeaf?.parent ?: return true
        return !previousParent.isLoopWithoutBody() && !previousParent.isIfExpressionWithoutThen()
    }

    private fun ASTNode.isLoopWithoutBody() =
        (elementType == WHILE || elementType == FOR) &&
            findChildByType(BODY)?.firstChildNode == null

    private fun ASTNode.isIfExpressionWithoutThen() = elementType == IF && findChildByType(THEN)?.firstChildNode == null

    private fun ASTNode?.doesNotRequirePreSemi(): Boolean =
        when {
            this == null -> true
            isWhiteSpace -> {
                nextLeaf {
                    it.isCode &&
                        it.findParentByType(KDOC) == null &&
                        it.findParentByType(ANNOTATION_ENTRY) == null
                }.let { nextCodeLeaf ->
                    nextCodeLeaf == null ||
                        (textContains('\n') && nextCodeLeaf.elementType != KtTokens.LBRACE)
                }
            }
            else -> false
        }

    companion object {
        const val ID = "ylcs:no-semi"
    }
}
