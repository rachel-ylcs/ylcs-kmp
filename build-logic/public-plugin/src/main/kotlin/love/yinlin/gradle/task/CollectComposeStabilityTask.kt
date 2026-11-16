package love.yinlin.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CollectComposeStabilityTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stabilityFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputFile.convention(project.layout.buildDirectory.file("generated/compose-stability/combined.conf"))
    }

    @TaskAction
    fun collect() {
        val lines = stabilityFiles.files.asSequence()
            .filter { it.isFile && it.extension.equals("conf", ignoreCase = true) }
            .flatMap { it.readLines(Charsets.UTF_8).asSequence() }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .sorted()
            .toList()

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(if (lines.isEmpty()) "" else lines.joinToString(separator = "\n", postfix = "\n"), Charsets.UTF_8)
    }
}
