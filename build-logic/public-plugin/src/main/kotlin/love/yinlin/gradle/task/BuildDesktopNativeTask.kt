package love.yinlin.gradle.task

import love.yinlin.gradle.BuildArchitecture
import love.yinlin.gradle.BuildPlatform
import love.yinlin.gradle.buildDefaults
import love.yinlin.gradle.cmakeCacheNeedsReset
import love.yinlin.gradle.defaultNativeModuleName
import love.yinlin.gradle.desktopNativeBuildDir
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "CMake output depends on the locally installed compiler and system libraries")
abstract class BuildDesktopNativeTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val additionalSourceDirs: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val nativeModuleName: Property<String>

    @get:Input
    abstract val nativePlatform: Property<BuildPlatform>

    @get:Input
    abstract val nativeArchitecture: Property<BuildArchitecture>

    @get:LocalState
    abstract val nativeBuildDir: DirectoryProperty

    @get:Internal
    abstract val nativeJniDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val nativeJniHeaders: ConfigurableFileCollection

    @get:Input
    abstract val cmakeOptions: ListProperty<Pair<String, String>>

    @get:Input
    abstract val preCommands: ListProperty<String>

    @get:Input
    abstract val preEnvironment: ListProperty<Pair<String, String>>

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val parallelism: Property<Int>

    @get:Input
    abstract val visualStudioDirectory: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        val defaults = project.buildDefaults
        val outputDirectory = defaults.native.outputDirectory?.let { project.layout.projectDirectory.dir(it) }
            ?: project.layout.buildDirectory.dir("native-libraries").get()
        nativeModuleName.convention(defaultNativeModuleName(project.path).ifEmpty { project.name })
        outputFile.convention(nativeModuleName.map { outputDirectory.file(System.mapLibraryName(it)) })
        nativePlatform.convention(defaults.platform)
        nativeArchitecture.convention(defaults.architecture)
        nativeBuildDir.convention(project.desktopNativeBuildDir)
        nativeJniDir.convention(defaults.native.jniHeadersDirectory?.let { project.layout.projectDirectory.dir(it) }
            ?: project.layout.buildDirectory.dir("native-include").get())
        nativeJniHeaders.from(project.fileTree(nativeJniDir))
        cmakeOptions.convention(emptyList())
        preCommands.convention(emptyList())
        preEnvironment.convention(emptyList())
        buildType.convention(defaults.native.buildType)
        parallelism.convention(defaults.native.parallelism)
        visualStudioDirectory.convention(project.providers.environmentVariable("VS_PATH").orElse(""))
    }

    fun ensureNativeBuild(inputDir: File): Boolean = inputDir.isDirectory && !inputDir.resolve("native.ignore").exists()

    @TaskAction
    fun buildNative() {
        val library = outputFile.get().asFile
        val output = library.parentFile
        output.mkdirs()
        val build = nativeBuildDir.get().asFile
        build.mkdirs()
        val platform = nativePlatform.get()
        val type = buildType.get()
        val fresh = if (cmakeCacheNeedsReset(build, inputDir.get().asFile)) "--fresh " else ""
        val options = buildList {
            add("CMAKE_BUILD_TYPE" to type)
            add("NATIVE_JNI_DIR" to "\"${nativeJniDir.get().asFile.absolutePath}\"")
            add("NATIVE_OUTPUT_DIR" to "\"${output.absolutePath}\"")
            add("NATIVE_OUTPUT_NAME" to nativeModuleName.get())
            add("RACHEL_CPP_SOURCE_DIRS" to "\"${additionalSourceDirs.files.joinToString(";") { it.invariantSeparatorsPath }}\"")
            if (platform == BuildPlatform.Windows) add("CMAKE_SHARED_LINKER_FLAGS" to "\"/NOEXP /NOIMPLIB\"")
            addAll(cmakeOptions.get())
        }.joinToString(" ") { (key, value) -> "-D$key=$value" }
        val commands = preCommands.get().toMutableList()
        if (platform == BuildPlatform.Windows) {
            val visualStudio = visualStudioDirectory.get()
            if (visualStudio.isEmpty()) throw GradleException("Can not find Visual Studio from VS_PATH environment variable!")
            val vcvars = File(visualStudio).resolve("VC/Auxiliary/Build/vcvars64.bat")
            if (!vcvars.isFile) throw GradleException("Can not find Visual Studio from VS_PATH environment variable!")
            commands.add(0, "call \"$vcvars\"")
            commands.add("cmake ${fresh}\"%NATIVE_SOURCE_DIR%\" -G Ninja $options")
        }
        else commands.add($$"cmake $$fresh\"${NATIVE_SOURCE_DIR}\" $$options")
        commands.add("cmake --build . --config $type --parallel ${parallelism.get()}")
        execOperations.exec {
            workingDir(build)
            environment("NATIVE_SOURCE_DIR", inputDir.get().asFile.absolutePath)
            for ((key, value) in preEnvironment.get()) environment(key, value)
            if (platform == BuildPlatform.Windows) commandLine("cmd", "/c", commands.joinToString(" && "))
            else commandLine("sh", "-c", commands.joinToString(" && "))
        }
        if (!library.isFile) throw GradleException("CMake did not produce the declared native library: $library")
        logger.lifecycle("[BuildDesktopNative] build \"${library.name}\" at ${library.absolutePath}")
    }
}
