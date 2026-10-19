package love.yinlin.gradle.task

import love.yinlin.gradle.BuildHost
import love.yinlin.gradle.buildDefaults
import love.yinlin.gradle.packageResourcesDir
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Copying local binaries is cheaper than retrieving them from a build cache")
abstract class CopyDesktopNativeTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val nativeLibraries: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val targetDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    init {
        val tag = project.buildDefaults.native.resourceTag ?: BuildHost.resourceTag
        targetDirectory.convention(project.packageResourcesDir.dir(tag))
    }

    @TaskAction
    fun copyNativeLibs() {
        fileSystemOperations.sync {
            from(nativeLibraries)
            into(targetDirectory)
        }
        logger.lifecycle("[CopyDesktopNative] copy desktop native library: ${nativeLibraries.files.map { it.name }.sorted()}")
    }
}
