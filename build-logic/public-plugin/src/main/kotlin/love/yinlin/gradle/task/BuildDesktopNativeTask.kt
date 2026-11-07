package love.yinlin.gradle.task

import love.yinlin.gradle.BuildArchitecture
import love.yinlin.gradle.BuildPlatform
import love.yinlin.gradle.KonanMingwToolchain
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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.Properties
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
    abstract val cmakeExecutable: Property<String>

    @get:Optional
    @get:Input
    abstract val ninjaExecutable: Property<String>

    @get:Internal
    abstract val nativeHome: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val compilerProperties: File? get() = nativeHome.file("konan/konan.properties").orNull?.asFile

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jdkHeaders: DirectoryProperty

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
        jdkHeaders.fileValue(File(System.getProperty("java.home"), "include"))
        cmakeOptions.convention(emptyList())
        preCommands.convention(emptyList())
        preEnvironment.convention(emptyList())
        buildType.convention(defaults.native.buildType)
        parallelism.convention(defaults.native.parallelism)
        cmakeExecutable.convention(defaults.native.cmakeExecutable)
        defaults.native.ninjaExecutable?.let { ninjaExecutable.convention(it) }
    }

    fun ensureNativeBuild(inputDir: File): Boolean = inputDir.isDirectory && !inputDir.resolve("native.ignore").exists()

    @TaskAction
    fun buildNative() {
        val library = outputFile.get().asFile
        val output = library.parentFile
        output.mkdirs()
        val platform = nativePlatform.get()
        val type = buildType.get()
        val environment = preEnvironment.get().toMap().toMutableMap()
        val jdk = jdkHeaders.get().asFile
        val jniPlatform = when (platform) {
            BuildPlatform.Windows -> "win32"
            BuildPlatform.Linux -> "linux"
            BuildPlatform.Mac -> "darwin"
        }
        check(jdk.resolve("jni.h").isFile && jdk.resolve("$jniPlatform/jni_md.h").isFile) { "Missing JNI headers in $jdk" }
        val include = nativeJniDir.get().asFile.invariantSeparatorsPath
        val options = mutableListOf(
            "-DCMAKE_BUILD_TYPE=$type", "-DRACHEL_DESKTOP_JNI=ON",
            "-DRACHEL_JNI_INCLUDE_DIRS=${jdk.invariantSeparatorsPath};${jdk.resolve(jniPlatform).invariantSeparatorsPath}",
            "-DNATIVE_JNI_DIR=$include", "-DRACHEL_CPP_INCLUDE_DIR=$include",
            "-DNATIVE_OUTPUT_NAME=${nativeModuleName.get()}",
            "-DRACHEL_CPP_SOURCE_DIRS=${additionalSourceDirs.files.joinToString(";") { it.invariantSeparatorsPath }}",
        )
        var build = nativeBuildDir.get().asFile
        if (platform == BuildPlatform.Windows) {
            val properties = Properties().apply { checkNotNull(compilerProperties).inputStream().use(::load) }
            build = build.resolve("mingw-${properties.getProperty("compilerVersion")}")
            val konanData = File(System.getenv("KONAN_DATA_DIR") ?: File(System.getProperty("user.home"), ".konan").path)
            val mingw = KonanMingwToolchain(properties, konanData.resolve("dependencies"), "mingw_x64")
            if (!mingw.ready) {
                val bootstrap = build.resolve("bootstrap").apply { mkdirs() }
                val definition = bootstrap.resolve("toolchain.def").apply {
                    writeText("headers = stdint.h\npackage = love.yinlin.desktopjni.toolchain\n")
                }
                val cinterop = nativeHome.get().asFile.resolve("bin/cinterop.bat")
                execOperations.exec {
                    commandLine("cmd", "/d", "/c", "call", cinterop.path, "-def", definition.path,
                        "-target", "mingw_x64", "-o", bootstrap.resolve("toolchain.klib").path)
                }
            }
            options += mingw.options()
            // The MinGW C++ runtime needs pthread even when the JNI library links it statically.
            options += listOf("-G", "Ninja", "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY",
                "-DCMAKE_SHARED_LINKER_FLAGS=-fuse-ld=lld -static -static-libgcc -static-libstdc++ -pthread")
            ninjaExecutable.orNull?.let { options += "-DCMAKE_MAKE_PROGRAM=${File(it).invariantSeparatorsPath}" }
            environment["PATH"] = mingw.path()
        }
        val cmakeOutput = if (platform == BuildPlatform.Windows) build.resolve("jni-output").apply { mkdirs() } else output
        options += "-DNATIVE_OUTPUT_DIR=${cmakeOutput.invariantSeparatorsPath}"
        options += cmakeOptions.get().map { (key, value) -> "-D$key=$value" }
        val source = inputDir.get().asFile
        build.mkdirs()
        val cmake = cmakeExecutable.get()
        if (cmakeCacheNeedsReset(build, source, cmake, options)) options += "--fresh"
        val configure = listOf(cmake, "-S", source.path, "-B", build.path) + options
        val compile = listOf(cmake, "--build", build.path, "--config", type, "--parallel", parallelism.get().toString())
        val before = preCommands.get()
        val commands = if (before.isEmpty()) listOf(configure, compile) else {
            val windows = platform == BuildPlatform.Windows
            val shell = if (windows) listOf("cmd", "/d", "/c") else listOf("sh", "-c")
            val cmakeCommands = listOf(configure, compile).map { args ->
                args.joinToString(" ") { arg -> if (windows) "\"$arg\"" else "'${arg.replace("'", "'\"'\"'")}'" }
            }
            listOf(shell + (before + cmakeCommands).joinToString(" && "))
        }
        for (command in commands) {
            execOperations.exec {
                workingDir(build)
                environment("NATIVE_SOURCE_DIR", source.absolutePath)
                environment(environment)
                commandLine(command)
            }
        }
        val produced = cmakeOutput.resolve(library.name)
        if (!produced.isFile || produced.length() == 0L) throw GradleException("CMake did not produce the declared native library: $produced")
        if (produced != library) produced.copyTo(library, overwrite = true)
        logger.lifecycle("[BuildDesktopNative] build \"${library.name}\" at ${library.absolutePath}")
    }
}
