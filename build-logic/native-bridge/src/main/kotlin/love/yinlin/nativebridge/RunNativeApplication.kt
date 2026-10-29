package love.yinlin.nativebridge

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Runs an interactive native process on every invocation")
abstract class RunNativeApplication : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distribution: DirectoryProperty
    @get:Input abstract val executableName: Property<String>
    @get:Input abstract val arguments: ListProperty<String>
    @get:Internal abstract val workingDirectory: DirectoryProperty
    @get:Inject abstract val execOperations: ExecOperations

    @TaskAction
    fun runApplication() {
        val directory = distribution.get().asFile
        val workspace = workingDirectory.get().asFile
        check(workspace.isDirectory || workspace.mkdirs()) { "Cannot create the Native application's working directory: $workspace" }
        execOperations.exec {
            workingDir = workspace
            commandLine(listOf(directory.resolve(executableName.get()).absolutePath) + arguments.get())
        }
    }
}
