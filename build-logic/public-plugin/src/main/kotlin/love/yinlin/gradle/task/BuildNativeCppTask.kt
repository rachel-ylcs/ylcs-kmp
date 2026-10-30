package love.yinlin.gradle.task

import love.yinlin.gradle.buildDefaults
import love.yinlin.gradle.cmakeCacheNeedsReset
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
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

    @get:Internal
    abstract val konanDataDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "native build"
        cmakeOptions.convention(emptyList())
        compilerHost.convention(HostManager.host.name)
        parallelism.convention(project.buildDefaults.native.parallelism.coerceAtMost(Runtime.getRuntime().availableProcessors().coerceAtLeast(1)))
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
        val environment = mutableMapOf<String, String>()
        val dependencies = konanDataDirectory.get().asFile.resolve("dependencies")
        val windowsHost = compilerHost.get() == "mingw_x64"
        val llvm = dependencies.resolve(properties.getProperty("llvm.${compilerHost.get()}.user"))
        val executableSuffix = if (windowsHost) ".exe" else ""
        val compiler = llvm.resolve("bin/clang$executableSuffix")
        val cppCompiler = llvm.resolve("bin/clang++$executableSuffix")
        val archiver = llvm.resolve("bin/llvm-ar$executableSuffix")
        if (windowsHost) {
            val visualStudio = System.getenv("VS_PATH") ?: System.getenv("SKIKO_VSBT_PATH")
            val ninja = visualStudio?.let { File(it).resolve("Common7/IDE/CommonExtensions/Microsoft/CMake/Ninja/ninja.exe") }
            if (ninja?.isFile == true) options += "-DCMAKE_MAKE_PROGRAM=${ninja.invariantSeparatorsPath}"
        }
        if (target == "mingw_x64") {
            val toolchain = dependencies.resolve(properties.getProperty("toolchainDependency.mingw_x64"))
            if (!compiler.isFile || !toolchain.isDirectory) bootstrapToolchain(work)
            check(compiler.isFile) { "Missing Kotlin/Native MinGW compiler: $compiler" }
            options += listOf("-DCMAKE_SYSTEM_NAME=Windows", "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
                "-DCMAKE_C_COMPILER=${compiler.invariantSeparatorsPath}", "-DCMAKE_CXX_COMPILER=${cppCompiler.invariantSeparatorsPath}",
                "-DCMAKE_C_COMPILER_TARGET=x86_64-w64-windows-gnu", "-DCMAKE_CXX_COMPILER_TARGET=x86_64-w64-windows-gnu",
                "-DCMAKE_SYSROOT=${toolchain.invariantSeparatorsPath}", "-DCMAKE_AR=${archiver.invariantSeparatorsPath}")
            environment["PATH"] = llvm.resolve("bin").path + File.pathSeparator + toolchain.resolve("bin").path + File.pathSeparator + System.getenv("PATH").orEmpty()
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
        else throw GradleException("Unsupported Native C++ target: $target")
        options += cmakeOptions.get().map { (key, value) -> "-D$key=$value" }
        val compilers = options.filter { it.startsWith("-DCMAKE_C_COMPILER=") || it.startsWith("-DCMAKE_CXX_COMPILER=") }
            .associate { it.substringBefore('=').removePrefix("-D") to it.substringAfter('=') }
        if (cmakeCacheNeedsReset(cmake, inputDir.get().asFile, compilers)) options += "--fresh"
        execute(listOf("cmake") + options, environment)
        execute(listOf("cmake", "--build", cmake.path, "--config", "Release", "--parallel", parallelism.get().toString()), environment)
        check(output.walkTopDown().any { it.isFile && (it.extension == "a" || it.extension == "lib") }) { "CMake did not produce a static archive in $output" }
    }
}
