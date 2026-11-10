package love.yinlin.lint

import java.io.File
import java.io.Writer

internal class KtlintReport(private val fileCount: Int, private val engineErrors: Int, findings: List<Finding>) {
    private val findings = findings.sortedWith(compareBy(Finding::path, Finding::line, Finding::column, Finding::rule))
    private val counts = findings.groupingBy { it.severity }.eachCount()

    fun write(directory: File, xlsxWriter: File) {
        directory.mkdirs()
        writeMarkdown(directory.resolve("ktlint.md"))
        writeExcel(directory.resolve("ktlint.xlsx"), xlsxWriter)
    }

    private fun writeMarkdown(file: File) {
        file.bufferedWriter().use { output ->
            output.appendLine("# Kotlin 规范检查")
            output.appendLine()
            output.appendLine("扫描文件 $fileCount 个，共发现 ${findings.size} 项（含解析或规则执行错误 $engineErrors 项）。")
            val summary = KtlintSeverity.entries.joinToString(" · ") { "${it.label} ${counts[it] ?: 0}" }
            output.appendLine(summary)
            output.appendLine()
            if (findings.isNotEmpty()) {
                output.appendLine("| 文件 | 行 | 列 | 规则 | 级别 | 说明 |")
                output.appendLine("| --- | ---: | ---: | --- | --- | --- |")
                for (finding in findings) {
                    output.appendLine("| ${escapeCell(finding.path)} | ${finding.line} | ${finding.column} | ${finding.rule} | ${finding.severity.label} | ${escapeCell(finding.message)} |")
                }
            }
        }
    }

    private fun writeExcel(file: File, script: File) {
        val process = ProcessBuilder("python", script.absolutePath, file.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        try {
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { output ->
                output.csvRow(fileCount, engineErrors)
                for (finding in findings) {
                    output.csvRow(finding.category, finding.path.substringBefore('/'), finding.path, finding.line, finding.column, finding.rule, finding.severity.label, finding.message)
                }
            }
            val exitCode = process.waitFor()
            check(exitCode == 0) { "Could not write ${file.absolutePath}. Python exited with code $exitCode." }
        } finally {
            if (process.isAlive) process.destroy()
        }
    }

    private fun Writer.csvRow(vararg values: Any) {
        appendLine(values.joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" })
    }

    private fun escapeCell(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("|", "&#124;").replace("`", "&#96;").replace('\r', ' ').replace('\n', ' ')

    data class Finding(
        val path: String,
        val line: Int,
        val column: Int,
        val rule: String,
        val severity: KtlintSeverity,
        val message: String,
        val category: String = when (rule.substringBefore(':')) {
            "standard" -> "标准规则"
            "internal" -> "抑制诊断"
            else -> "项目规则"
        }
    )
}
