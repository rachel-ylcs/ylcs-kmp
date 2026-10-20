package love.yinlin.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Writing a small source file is cheaper than retrieving it from a build cache")
abstract class GenerateCodeTask : DefaultTask() {
    @get:Input
    abstract val code: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/kotlin"))
        outputFile.convention(outputDirectory.file(className.map { name ->
            require(name.split('.').all { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) }) { "Invalid generated class name: $name" }
            "${name.replace('.', '/')}.kt"
        }))
    }

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(code.get(), Charsets.UTF_8)
        logger.lifecycle("[GenerateCode] generated code for ${className.get()} in ${file.absolutePath}")
    }
}
