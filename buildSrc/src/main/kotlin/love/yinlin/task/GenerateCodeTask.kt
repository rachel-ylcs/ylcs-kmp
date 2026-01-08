package love.yinlin.task

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
    abstract val title: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(code.get(), Charsets.UTF_8)
        println("Generated code for ${title.get()}: ${file.absolutePath}")
    }
}