package love.yinlin.task

import BuildPlatform
import C
import desktopNativeBuildDir
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.environment
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class BuildDesktopNativeTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val nativeModuleName: Property<String>

    @get:Input
    abstract val nativePlatform: Property<BuildPlatform>

    @get:Input
    abstract val nativeBuildDir: Property<File>

    @get:Input
    abstract val nativeJniDir: Property<File>

    @get:Input
    abstract val cmakeOptions: ListProperty<Pair<String, String>>

    @get:Input
    abstract val preCommands: ListProperty<String>

    @get:Input
    abstract val preEnvironment: ListProperty<Pair<String, String>>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        nativeModuleName.convention(project.name) // 非当前项目的 Task 需要显式传递 nativeModuleName
        outputFile.convention(nativeModuleName.map { newName ->
            project.C.root.artifacts.desktopNative.file(System.mapLibraryName(newName.replace('-', '_')))
        })
        nativePlatform.convention(project.C.platform)
        nativeBuildDir.convention(project.desktopNativeBuildDir.asFile) // 非当前项目的 Task 需要显式传递 nativeBuildDir
        nativeJniDir.convention(project.C.root.artifacts.include.asFile)
        cmakeOptions.convention(emptyList())
        preCommands.convention(emptyList())
        preEnvironment.convention(emptyList())
    }

    private fun buildArgs(vararg args: String) = args.joinToString(" && ")

    private fun ExecOperations.exec(
        currentDir: File,
        env: List<Pair<String, String>>,
        vararg args: String
    ): Boolean {
        return exec {
            workingDir = currentDir
            standardOutput = System.out
            errorOutput = System.err
            environment(*env.toTypedArray())
            commandLine(*args)
        }.exitValue == 0
    }

    @TaskAction
    fun buildNative() {
        // 准备输出目录
        val libFile = outputFile.get().asFile
        val libOutputDir = libFile.parentFile
        if (!libOutputDir.exists()) libOutputDir.mkdirs()

        // 准备 native 库编译目录
        val buildDir = nativeBuildDir.get()
        buildDir.mkdirs()

        // 设置 cmake 参数
        val cmakelistsDir = inputDir.get().asFile
        val currentPlatform = nativePlatform.get()
        val cmakeArgs = buildList {
            add("CMAKE_BUILD_TYPE" to "Release")
            add("NATIVE_JNI_DIR" to "\"${nativeJniDir.get().absolutePath}\"")
            add("NATIVE_OUTPUT_DIR" to "\"${libOutputDir.absolutePath}\"")
            add("NATIVE_OUTPUT_NAME" to nativeModuleName.get().replace('-', '_'))
            if (currentPlatform == BuildPlatform.Windows) {
                add("CMAKE_SHARED_LINKER_FLAGS" to "\"/NOEXP /NOIMPLIB\"")
            }
            addAll(cmakeOptions.get())
        }
        val cmakeArgsText = cmakeArgs.joinToString(" ") { (key, value) -> "-D$key=$value" }

        // 调用工具链编译 Native 库
        if (currentPlatform == BuildPlatform.Windows) {
            // 检查工具链
            val vsPath = System.getenv("VS_PATH") ?: throw GradleException("can not find Visual Studio from VS_PATH environment variable!")
            val vcPath = File(vsPath).resolve("VC/Auxiliary/Build/vcvars64.bat")
            if (!vcPath.exists()) throw GradleException("Can not find Visual Studio from \"VS_PATH\" environment variable!")

            // Windows 调用 Ninja 使用 MSBuild 构建
            execOperations.exec(buildDir, listOf(
                "NATIVE_SOURCE_DIR" to cmakelistsDir.absolutePath
            ) + preEnvironment.get(), "cmd", "/c", buildArgs(
                "call \"$vcPath\"",
                *preCommands.get().toTypedArray(),
                "cmake \"%NATIVE_SOURCE_DIR%\" -G Ninja $cmakeArgsText",
                "cmake --build . --config Release"
            ))
        }
        else {
            // Linux / macOS 直接 build
            execOperations.exec(buildDir, listOf(
                "NATIVE_SOURCE_DIR" to cmakelistsDir.absolutePath
            ) + preEnvironment.get(), "sh", "-c", buildArgs(
                *preCommands.get().toTypedArray(),
                $$"cmake \"${NATIVE_SOURCE_DIR}\" $$cmakeArgsText",
                "make -j 4"
            ))
        }
        println("[BuildDesktopNative] build \"${libFile.name}\" at ${libFile.absolutePath}")
    }
}