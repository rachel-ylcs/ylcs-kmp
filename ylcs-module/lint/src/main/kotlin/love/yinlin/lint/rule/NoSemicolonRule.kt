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
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
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
        if (node.elementType != SEMICOLON) return

        val nextLeaf = node.nextLeaf
        val isEnumEntryTerminator = node.parent?.elementType == ENUM_ENTRY
        if (nextLeaf.doesNotRequirePreSemi() && isNoSemicolonRequiredAfter(node)) {
            val _ = emit(node.startOffset, "Unnecessary semicolon", false)
        } else if (!nextLeaf.isWhiteSpace && !(isEnumEntryTerminator && nextLeaf?.elementType == RBRACE)) {
            if (node.prevLeaf.isWhiteSpaceWithNewline) return
            val _ = emit(node.startOffset + 1, "Missing spacing after \";\"", false)
        }
    }

    private fun ASTNode?.doesNotRequirePreSemi(): Boolean =
        when {
            this == null -> true
            isWhiteSpace -> {
                nextLeaf {
                    it.isCode &&
                        it.findParentByType(KDOC) == null &&
                        it.findParentByType(ANNOTATION_ENTRY) == null
                }.let { nextLeaf ->
                    nextLeaf == null ||
                        (textContains('\n') && nextLeaf.elementType != KtTokens.LBRACE)
                }
            }
            else -> false
        }

    private fun isNoSemicolonRequiredAfter(node: ASTNode): Boolean {
        node.prevCodeLeaf
            ?.also { prevCodeLeaf ->
                if (prevCodeLeaf.elementType == OBJECT_KEYWORD) return false
            }?.parent
            ?.run {
                if (isLoopWithoutBody()) return false
                if (isIfExpressionWithoutThen()) return false
            }

        // A semicolon after an enum entry separates enum values from class members.
        // Keep it even when the enum has no members after it.
        if (node.parent?.elementType == ENUM_ENTRY) return false
        if (node.isEnumClassWithoutValues()) return false

        return true
    }

    private fun ASTNode.isLoopWithoutBody() =
        (elementType == WHILE || elementType == FOR) &&
            findChildByType(BODY)?.firstChildNode == null

    private fun ASTNode.isIfExpressionWithoutThen() = elementType == IF && findChildByType(THEN)?.firstChildNode == null

    private fun ASTNode?.isLastCodeLeafBeforeClosingOfClassBody() =
        getLastCodeLeafBeforeClosingOfClassBody() == this

    private fun ASTNode?.getLastCodeLeafBeforeClosingOfClassBody() =
        this
            ?.findParentByType(CLASS_BODY)
            ?.lastChildLeafOrSelf
            ?.prevCodeLeaf

    private fun ASTNode?.isEnumClassWithoutValues() =
        this
            ?.takeIf { !it.isLastCodeLeafBeforeClosingOfClassBody() }
            ?.findParentByType(CLASS_BODY)
            ?.takeIf { this == it.firstChildNode.nextCodeSibling }
            ?.findParentByType(CLASS)
            ?.hasModifier(ENUM_KEYWORD)
            ?: false

    companion object {
        const val ID = "ylcs:no-semi"
    }
}
