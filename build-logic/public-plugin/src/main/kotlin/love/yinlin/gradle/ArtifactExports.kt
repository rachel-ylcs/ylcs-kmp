package love.yinlin.gradle

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/** Exports build outputs. The source providers carry the tasks that produce them. */
class ArtifactExports internal constructor(private val project: Project) {
    fun copy(taskName: String, source: Provider<RegularFile>, destination: Directory, fileName: String? = null, description: String): TaskProvider<Copy> =
        project.tasks.register<Copy>(taskName) {
            this.description = description
            from(source)
            into(destination)
            if (fileName != null) rename { fileName }
        }

    fun zip(
        taskName: String,
        source: Provider<Directory>,
        destination: Directory,
        fileName: String,
        description: String,
        archiveRoot: String,
        excludes: List<String> = emptyList(),
        executable: Provider<RegularFile>? = null,
        replaceUnpackedOutput: Boolean = false,
    ): TaskProvider<out Zip> {
        require(archiveRoot.isNotBlank() && archiveRoot == archiveRoot.trimEnd(' ', '.') &&
            archiveRoot.none { it < ' ' || it in "/\\:" }) {
            "The archive root must be a single directory name: $archiveRoot"
        }
        return project.tasks.register<ArtifactZip>(taskName) {
            this.description = description
            from(source) {
                into(archiveRoot)
                exclude(excludes)
            }
            destinationDirectory.set(destination)
            archiveFileName.set(fileName)
            if (replaceUnpackedOutput) previousDirectory.set(destination.dir(archiveRoot))
            if (executable != null) {
                useFileSystemPermissions()
                inputs.file(executable).withPropertyName("desktopExecutable")
            }
        }
    }

    fun sync(taskName: String, source: Provider<Directory>, destination: Directory, description: String): TaskProvider<Sync> =
        project.tasks.register<Sync>(taskName) {
            this.description = description
            from(source)
            into(destination)
        }

    fun aggregate(taskName: String, description: String, vararg exports: TaskProvider<*>) {
        project.tasks.register(taskName) {
            this.description = description
            dependsOn(*exports)
        }
    }
}

@DisableCachingByDefault(because = "Exports a local distribution archive")
internal abstract class ArtifactZip : Zip() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Internal
    abstract val previousDirectory: DirectoryProperty

    init {
        doFirst {
            previousDirectory.orNull?.let { directory -> fileSystem.delete { delete(directory) } }
        }
    }
}
