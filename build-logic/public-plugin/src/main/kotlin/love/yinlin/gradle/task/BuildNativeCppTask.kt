package love.yinlin.gradle.task

import love.yinlin.gradle.buildDefaults
import love.yinlin.gradle.cmakeCacheNeedsReset
import love.yinlin.gradle.KonanMingwToolchain
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.util.Properties
import javax.inject.Inject

@DisableCachingByDefault(because = "CMake archives depend on the selected host compiler and Kotlin/Native SDK")
abstract class BuildNativeCppTask : DefaultTask() {
    @get:Internal
    abstract val inputDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: FileTree get() = inputDir.asFileTree.matching { exclude("**/.git", "**/.git/**", "**/build/**") }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val extraSourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val nativeTarget: Property<String>

    @get:Input
    abstract val compilerHost: Property<String>

    @get:Internal
    abstract val nativeHome: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val compilerProperties: File get() = nativeHome.file("konan/konan.properties").get().asFile

    @get:OutputDirectory
    abstract val libraryDirectory: DirectoryProperty

    @get:LocalState
    abstract val cmakeBuildDirectory: DirectoryProperty

    @get:Input
    abstract val cmakeOptions: ListProperty<Pair<String, String>>

    @get:Input
    abstract val parallelism: Property<Int>

    @get:Input
    abstract val cmakeExecutable: Property<String>

    @get:Optional
    @get:Input
    abstract val ninjaExecutable: Property<String>

    @get:Internal
    abstract val konanDataDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "native build"
        cmakeOptions.convention(emptyList())
        compilerHost.convention(HostManager.host.name)
        val defaults = project.buildDefaults.native
        parallelism.convention(defaults.parallelism.coerceAtMost(Runtime.getRuntime().availableProcessors().coerceAtLeast(1)))
        cmakeExecutable.convention(defaults.cmakeExecutable)
        defaults.ninjaExecutable?.let { ninjaExecutable.convention(it) }
        konanDataDirectory.convention(project.layout.dir(project.providers.provider {
            File(System.getenv("KONAN_DATA_DIR") ?: File(System.getProperty("user.home"), ".konan").path)
        }))
    }

    private fun execute(arguments: List<String>, environment: Map<String, String> = emptyMap()) {
        execOperations.exec {
            commandLine(arguments)
            environment(environment)
        }
    }

    private fun bootstrapToolchain(work: File) {
        val bootstrap = work.resolve("bootstrap").apply { mkdirs() }
        val definition = bootstrap.resolve("toolchain.def")
        definition.writeText("headers = stdint.h\npackage = love.yinlin.nativecpp.toolchain\n")
        val windows = compilerHost.get() == "mingw_x64"
        val executable = nativeHome.get().asFile.resolve(if (windows) "bin/cinterop.bat" else "bin/cinterop")
        val arguments = listOf(executable.absolutePath, "-def", definition.absolutePath, "-target", nativeTarget.get(), "-o", bootstrap.resolve("toolchain.klib").absolutePath)
        execute(if (windows) listOf("cmd", "/d", "/c") + arguments else arguments)
    }

    @TaskAction
    fun buildNative() {
        val target = nativeTarget.get()
        val work = cmakeBuildDirectory.get().asFile.apply { mkdirs() }
        val output = libraryDirectory.get().asFile.apply { mkdirs() }
        val properties = Properties().apply { compilerProperties.inputStream().use(::load) }
        val cmake = work.resolve("cmake-${properties.getProperty("compilerVersion")}")
        val options = mutableListOf("-S", inputDir.get().asFile.path, "-B", cmake.path, "-G", "Ninja", "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_ARCHIVE_OUTPUT_DIRECTORY=${output.invariantSeparatorsPath}", "-DCMAKE_POSITION_INDEPENDENT_CODE=ON", "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY", "-DRACHEL_NATIVE_TARGET=$target")
        ninjaExecutable.orNull?.let { options += "-DCMAKE_MAKE_PROGRAM=${File(it).invariantSeparatorsPath}" }
        val environment = mutableMapOf<String, String>()
        val dependencies = konanDataDirectory.get().asFile.resolve("dependencies")
        val windowsHost = compilerHost.get() == "mingw_x64"
        val llvm = dependencies.resolve(properties.getProperty("llvm.${compilerHost.get()}.user"))
        val executableSuffix = if (windowsHost) ".exe" else ""
        val compiler = llvm.resolve("bin/clang$executableSuffix")
        val cppCompiler = llvm.resolve("bin/clang++$executableSuffix")
        val archiver = llvm.resolve("bin/llvm-ar$executableSuffix")
        if (target == "mingw_x64") {
            val mingw = KonanMingwToolchain(properties, dependencies, compilerHost.get())
            if (!mingw.ready) bootstrapToolchain(work)
            options += mingw.options()
            environment["PATH"] = mingw.path()
        }
        else if (target == "linux_x64") {
            val toolchain = dependencies.resolve(properties.getProperty("toolchainDependency.linux_x64"))
            val sysroot = toolchain.resolve("x86_64-unknown-linux-gnu/sysroot")
            if (!compiler.isFile || !sysroot.isDirectory) bootstrapToolchain(work)
            check(compiler.isFile && sysroot.isDirectory) { "Missing Kotlin/Native Linux toolchain" }
            options += listOf("-DCMAKE_SYSTEM_NAME=Linux", "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
                "-DCMAKE_C_COMPILER=${compiler.invariantSeparatorsPath}", "-DCMAKE_CXX_COMPILER=${cppCompiler.invariantSeparatorsPath}", "-DCMAKE_AR=${archiver.invariantSeparatorsPath}",
                "-DCMAKE_C_COMPILER_TARGET=x86_64-unknown-linux-gnu", "-DCMAKE_CXX_COMPILER_TARGET=x86_64-unknown-linux-gnu",
                "-DCMAKE_C_COMPILER_EXTERNAL_TOOLCHAIN=${toolchain.invariantSeparatorsPath}", "-DCMAKE_CXX_COMPILER_EXTERNAL_TOOLCHAIN=${toolchain.invariantSeparatorsPath}", "-DCMAKE_SYSROOT=${sysroot.invariantSeparatorsPath}")
        }
        else if (target == "macos_arm64") options += listOf("-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0")
        else if (target == "ios_arm64" || target == "ios_simulator_arm64") {
            val sdk = if (target == "ios_arm64") "iphoneos" else "iphonesimulator"
            options += listOf("-DCMAKE_SYSTEM_NAME=iOS", "-DCMAKE_OSX_SYSROOT=$sdk", "-DCMAKE_OSX_ARCHITECTURES=arm64")
        }
        else throw GradleException("Unsupported Native C++ target: $target")
        options += cmakeOptions.get().map { (key, value) -> "-D$key=$value" }
        val executable = cmakeExecutable.get()
        if (cmakeCacheNeedsReset(cmake, inputDir.get().asFile, executable, options)) options += "--fresh"
        execute(listOf(executable) + options, environment)
        execute(listOf(executable, "--build", cmake.path, "--config", "Release", "--parallel", parallelism.get().toString()), environment)
        check(output.walkTopDown().any { it.isFile && (it.extension == "a" || it.extension == "lib") }) { "CMake did not produce a static archive in $output" }
    }
}
