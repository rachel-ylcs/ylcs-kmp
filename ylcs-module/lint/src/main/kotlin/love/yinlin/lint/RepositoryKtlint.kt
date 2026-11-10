package love.yinlin.lint

import io.github.ktlint.core.rule.engine.api.Code
import io.github.ktlint.core.rule.engine.api.EditorConfigOverride.Companion.plus
import io.github.ktlint.core.rule.engine.api.KtLintParseException
import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import io.github.ktlint.core.rule.engine.api.KtLintRuleException
import io.github.ktlint.core.rule.engine.core.api.editorconfig.ALL_RULES_EXECUTION_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.RuleExecution
import io.github.ktlint.core.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import io.github.ktlint.core.ruleset.standard.StandardRuleSetProvider
import java.io.File

internal object RepositoryKtlint {
    private const val SUPPRESSION_RULE = "internal:ktlint-suppression"

    @JvmStatic
    fun main(args: Array<String>) {
        val (repositoryPath, reportPath, sourceListPath) = args
        val root = File(repositoryPath)
        val report = File(reportPath)
        val sources = File(sourceListPath).readLines()
        check(sources.isNotEmpty()) { "No Kotlin sources found in ylcs-module or ylcs-app" }

        val providers = StandardRuleSetProvider().getRuleProviders() + RepositoryKtlintRules.custom
        val ruleIds = providers.map { it.ruleId.value }.toSet() + SUPPRESSION_RULE
        val rules = RepositoryKtlintRules.rules.associateBy { it.name }
        check(ruleIds.size == providers.size + 1 && rules.size == RepositoryKtlintRules.rules.size) { "Duplicate ktlint rule IDs" }
        check(ruleIds.containsAll(rules.keys)) {
            "Unknown ktlint rules in RepositoryKtlintRules: ${rules.keys - ruleIds}"
        }

        val ruleOverrides = providers.map { provider ->
            val execution = if (rules[provider.ruleId.value]?.enabled == true) RuleExecution.enabled else RuleExecution.disabled
            provider.ruleId.createRuleExecutionEditorConfigProperty() to execution
        }
        val executionOverrides = ruleOverrides + (ALL_RULES_EXECUTION_PROPERTY to RuleExecution.enabled)
        val engine = KtLintRuleEngine(
            ruleProviders = providers,
            editorConfigOverride = RepositoryKtlintRules.options.plus(*executionOverrides.toTypedArray())
        )

        val findings = mutableListOf<Finding>()
        var engineErrors = 0
        for (path in sources) {
            try {
                engine.lint(Code.fromFile(root.resolve(path))) { error ->
                    val rule = rules[error.ruleId.value]
                    // 抑制规则由 ktlint 始终执行，enabled 只控制其诊断是否进入报告。
                    if (rule?.enabled == true) {
                        findings.add(Finding(path, error.line, error.col, rule.name, rule.severity, error.detail))
                    }
                }
            } catch (error: KtLintParseException) {
                engineErrors++
                findings.add(Finding(path, error.line, error.col, "ktlint:parse", KtlintSeverity.ERROR, error.message.orEmpty()))
            } catch (error: KtLintRuleException) {
                engineErrors++
                findings.add(Finding(path, error.line, error.col, error.ruleId, KtlintSeverity.ERROR, error.message))
            }
        }

        report.parentFile.mkdirs()
        report.bufferedWriter().use { output ->
            output.appendLine("# Kotlin 规范检查")
            output.appendLine()
            output.appendLine("扫描文件 ${sources.size} 个，共发现 ${findings.size} 项（含解析或规则执行错误 ${engineErrors} 项）。")
            val counts = findings.groupingBy { it.severity }.eachCount()
            val summary = KtlintSeverity.entries
                .joinToString(" · ") { "${it.label} ${counts[it] ?: 0}" }
            output.appendLine(summary)
            output.appendLine()
            if (findings.isNotEmpty()) {
                output.appendLine("| 文件 | 行 | 列 | 规则 | 级别 | 说明 |")
                output.appendLine("| --- | ---: | ---: | --- | --- | --- |")
                for (finding in findings.sortedWith(compareBy(Finding::path, Finding::line, Finding::column, Finding::rule))) {
                    output.appendLine("| ${escapeCell(finding.path)} | ${finding.line} | ${finding.column} | ${finding.rule} | ${finding.severity.label} | ${escapeCell(finding.message)} |")
                }
            }
        }
        println("ktlint: ${sources.size} files, ${findings.size} findings. Report: ${report.absolutePath}")
        check(engineErrors == 0) { "ktlint could not complete $engineErrors checks. See ${report.absolutePath}" }
    }

    private fun escapeCell(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("|", "&#124;").replace("`", "&#96;").replace('\r', ' ').replace('\n', ' ')

    private data class Finding(
        val path: String,
        val line: Int,
        val column: Int,
        val rule: String,
        val severity: KtlintSeverity,
        val message: String
    )
}
