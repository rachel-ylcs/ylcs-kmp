package love.yinlin.task

import generateSourceDir
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateCodeTask : DefaultTask() {
    @get:Input
    abstract val code: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputFile.convention {
            val classPath = className.get().split('.')
            var outputFilePath = project.generateSourceDir
            for (i in 0 ..< classPath.size - 1) outputFilePath = outputFilePath.dir(classPath[i])
            outputFilePath.file("${classPath.last()}.kt").asFile
        }
    }

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(code.get(), Charsets.UTF_8)
        println("[GenerateCode] generated code for ${className.get()} in ${file.absolutePath}")
    }
}