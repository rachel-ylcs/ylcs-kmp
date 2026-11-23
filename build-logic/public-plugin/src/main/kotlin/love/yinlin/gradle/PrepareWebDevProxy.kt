package love.yinlin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Writing a small local webpack configuration is cheaper than retrieving it from a build cache")
abstract class PrepareWebDevProxy : DefaultTask() {
    @get:Input
    abstract val script: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val folder = outputDirectory.get().asFile
        folder.mkdirs()
        folder.resolve("web-dev-proxy.js").writeText(script.get())
    }
}
