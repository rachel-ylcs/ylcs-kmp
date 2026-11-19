package love.yinlin.lint.rule

import io.github.ktlint.core.rule.engine.api.Code
import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class NoSemicolonRuleTest {
    @Test
    fun enumEntryTerminatorIsAllowedWithoutMembers() {
        assertTrue(lint("enum class Direction { NORTH; }").isEmpty())
        assertTrue(lint("enum class Direction { NORTH;}").isEmpty())
    }

    @Test
    fun enumEntryTerminatorIsAllowedWithMembers() {
        val code =
            """
            enum class Direction {
                NORTH,
                SOUTH;

                fun isNorth() = this == NORTH
            }
            """.trimIndent()
        assertTrue(lint(code).isEmpty())
    }

    @Test
    fun unnecessarySemicolonOutsideEnumIsStillReported() {
        assertEquals(listOf("Unnecessary semicolon"), lint("fun foo() {};"))
    }

    private fun lint(code: String): List<String> {
        val findings = mutableListOf<String>()
        val engine = KtLintRuleEngine(ruleProviders = setOf(RuleV2Provider { NoSemicolonRule() }))
        engine.lint(Code.fromSnippet(code)) { findings.add(it.detail) }
        return findings
    }
}
