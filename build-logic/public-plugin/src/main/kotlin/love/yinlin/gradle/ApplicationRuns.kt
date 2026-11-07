package love.yinlin.gradle

import love.yinlin.nativebridge.NativeApplicationExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

internal enum class RunMode(val nativeBuildType: NativeBuildType) {
    Debug(NativeBuildType.DEBUG),
    Release(NativeBuildType.RELEASE),
}

internal data class RunAlias(val taskName: String, val description: String)

/** Options for Gradle-launched processes. They are independent of application packaging. */
open class ProcessRun internal constructor() {
    var arguments: List<String> = emptyList()
    var workingDirectory: File? = null
    var libraryDirectories: List<File> = emptyList()
    var environment: Map<String, String> = emptyMap()
    internal val modes = linkedMapOf<RunMode, RunAlias?>()

    fun debug(taskName: String? = null, description: String = "Run the Debug application") {
        modes[RunMode.Debug] = taskName?.let { RunAlias(it, description) }
    }

    fun release(taskName: String? = null, description: String = "Run the Release application") {
        modes[RunMode.Release] = taskName?.let { RunAlias(it, description) }
    }
}

class JvmRun internal constructor() : ProcessRun() {
    var jvmArgs: List<String> = emptyList()
    var buildDesktopNativeLibraries: Boolean = false
}

/** Configures existing run tasks and optionally gives them project-specific aliases. */
class ApplicationRuns internal constructor(private val project: Project) {
    fun jvm(block: JvmRun.() -> Unit) {
        val run = JvmRun().apply(block)
        val nativeLibraries = if (run.buildDesktopNativeLibraries) project.providers.provider { project.desktopNativeLibraries() } else null
        for ((mode, alias) in run.modes.ifEmpty { mapOf(RunMode.Debug to null) }) {
            val producer = project.tasks.named<JavaExec>(if (mode == RunMode.Debug) "run" else "runRelease")
            producer.configure {
                if (nativeLibraries != null) dependsOn(nativeLibraries)
                args(run.arguments)
                jvmArgs(run.jvmArgs)
                environment(run.environment)
                if (run.libraryDirectories.isNotEmpty()) systemProperty("java.library.path", libraryPath(run))
                run.workingDirectory?.let { workspace ->
                    workingDir(workspace)
                    doFirst { ensureRunDirectory(workspace) }
                }
            }
            registerAlias(alias, producer)
        }
    }

    fun nativeDesktop(block: ProcessRun.() -> Unit) {
        val run = ProcessRun().apply(block)
        val application = project.extensions.getByType<NativeApplicationExtension>()
        for ((mode, alias) in run.modes.ifEmpty { RunMode.entries.associateWith { null } }) {
            application.findHostRunTask(mode.nativeBuildType)?.configure {
                arguments.addAll(run.arguments)
                run.workingDirectory?.let { workingDirectory.fileValue(it) }
                environmentVariables.putAll(run.environment)
                if (run.libraryDirectories.isNotEmpty()) {
                    environmentVariables.put(nativeLibraryVariable(), nativeLibraryPath(run))
                }
            }
            val entry = if (mode == RunMode.Debug) "composeRun" else "composeReleaseRun"
            if (alias != null) registerAlias(alias, project.tasks.named(entry))
        }
    }

    fun nativeExecutable(block: ProcessRun.() -> Unit) {
        val run = ProcessRun().apply(block)
        val targets = project.extensions.getByType<KotlinMultiplatformExtension>().targets.withType<KotlinNativeTarget>()
        for ((mode, alias) in run.modes.ifEmpty { RunMode.entries.associateWith { null } }) {
            for (target in targets) {
                val producer = target.binaries.findExecutable(mode.nativeBuildType)?.runTaskProvider ?: continue
                producer.configure { configureNativeExecutable(run) }
                if (target.konanTarget == HostManager.host) registerAlias(alias, producer)
            }
        }
    }

    fun browser(taskName: String, target: BrowserArtifactTarget, description: String) {
        registerAlias(RunAlias(taskName, description), project.tasks.named("${target.taskPrefix}DevelopmentRun"))
    }

    private fun AbstractExecTask<*>.configureNativeExecutable(run: ProcessRun) {
        args(run.arguments)
        environment(run.environment)
        if (run.libraryDirectories.isNotEmpty()) environment(nativeLibraryVariable(), nativeLibraryPath(run).get())
        run.workingDirectory?.let { workspace ->
            workingDir(workspace)
            doFirst { ensureRunDirectory(workspace) }
        }
    }

    private fun nativeLibraryVariable(): String = when (project.buildDefaults.platform) {
        BuildPlatform.Windows -> "PATH"
        BuildPlatform.Linux -> "LD_LIBRARY_PATH"
        BuildPlatform.Mac -> "DYLD_LIBRARY_PATH"
    }

    private fun nativeLibraryPath(run: ProcessRun): Provider<String> {
        val name = nativeLibraryVariable()
        val paths = libraryPath(run)
        val existing = run.environment[name]?.let { value -> project.providers.provider { value } } ?: project.providers.environmentVariable(name)
        return existing.map { if (it.isEmpty()) paths else "$paths${File.pathSeparator}$it" }.orElse(paths)
    }

    private fun registerAlias(alias: RunAlias?, producer: TaskProvider<*>) {
        if (alias == null) return
        project.tasks.register(alias.taskName) {
            description = alias.description
            dependsOn(producer)
        }
    }
}

private fun libraryPath(run: ProcessRun): String = run.libraryDirectories.joinToString(File.pathSeparator) { it.absolutePath }

private fun ensureRunDirectory(directory: File) {
    check(directory.isDirectory || directory.mkdirs() || directory.isDirectory) { "Cannot create the application's working directory: $directory" }
}
