package love.yinlin.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.StringWriter
import java.util.Properties

@CacheableTask
abstract class PrepareDokkaCInteropTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val definitionFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun prepare() {
        val source = definitionFile.get().asFile.readText().replace("\r\n", "\n")
        val separator = Regex("(?m)^---[ \\t]*$").find(source)
        val properties = Properties().apply { load(source.substring(0, separator?.range?.first ?: source.length).reader()) }
        properties.stringPropertyNames().filter { it == "staticLibraries" || it.startsWith("staticLibraries.") }.forEach(properties::remove)
        val serialized = StringWriter().also { properties.store(it, null) }.toString().lineSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.sorted().joinToString("\n", postfix = "\n")
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.bufferedWriter().use { writer ->
            writer.write(serialized)
            if (separator != null) writer.write(source.substring(separator.range.first))
        }
    }
}
