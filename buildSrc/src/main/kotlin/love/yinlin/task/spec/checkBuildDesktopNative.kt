package love.yinlin.task.spec

import C
import KotlinJvmTemplate
import KotlinMultiplatformTemplate
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import java.io.File

fun KotlinJvmTemplate.checkBuildDesktopNative(project: Project) = project.buildDesktopNative("src/main/cpp", "jar")
fun KotlinMultiplatformTemplate.checkBuildDesktopNative(project: Project) = project.buildDesktopNative("src/desktopMain/cpp", "desktopJar")

private fun buildArgs(vararg args: String) = args.joinToString(" && ")

private fun ExecOperations.exec(currentDir: File, vararg args: String): Boolean {
    return exec {
        workingDir = currentDir
        standardOutput = System.out
        errorOutput = System.err
        commandLine(*args)
    }.exitValue == 0
}

fun Project.buildDesktopNative(nativePath: String, jarTaskName: String) {
    // 检查是否包含 native 代码
    val projectName = project.name
    val moduleDir = layout.projectDirectory.asFile
    val sourceDir = moduleDir.resolve(nativePath)
    if (!sourceDir.exists()) return
    tasks.named(jarTaskName) {
        doLast {
            // 确定输出目录
            val outputDir = C.root.artifacts.desktopNative.asFile
            outputDir.mkdirs()

            // 检查 native 库是否生成
            val moduleName = projectName.replace('-', '_')
            val libName = System.mapLibraryName(moduleName)
            val outputLibFile = outputDir.resolve(libName)
            if (outputLibFile.exists()) return@doLast

            // 准备 native 库编译目录
            val nativeBuildDir = sourceDir.resolve("build")
            nativeBuildDir.deleteRecursively()
            nativeBuildDir.mkdir()

            // 设置 cmake 参数
            val cmakeArgs = buildList {
                add("CMAKE_BUILD_TYPE" to "Release")
                add("NATIVE_JNI_DIR" to "\"${C.root.artifacts.include.asFile.absolutePath}\"")
                add("NATIVE_OUTPUT_DIR" to outputDir.absolutePath)
                add("NATIVE_OUTPUT_NAME" to moduleName)
                if (C.platform == BuildPlatform.Windows) {
                    add("CMAKE_SHARED_LINKER_FLAGS" to "\"/NOEXP /NOIMPLIB\"")
                }
            }
            val cmakeArgsText = cmakeArgs.joinToString(" ") { (key, value) -> "-D$key=$value" }

            // 调用工具链编译 Native 库
            val execOpt = serviceOf<ExecOperations>()
            if (C.platform == BuildPlatform.Windows) {
                // 检查工具链
                val vsPath = System.getenv("VS_PATH") ?: throw GradleException("can not find Visual Studio from VS_PATH environment variable!")
                val vcPath = File(vsPath).resolve("VC/Auxiliary/Build/vcvars64.bat")
                if (!vcPath.exists()) throw GradleException("Can not find Visual Studio from \"VS_PATH\" environment variable!")

                // Windows 调用 Ninja 使用 MSBuild 构建
                execOpt.exec(nativeBuildDir, "cmd", "/c", buildArgs(
                    "call $vcPath",
                    "cmake .. -G Ninja $cmakeArgsText",
                    "cmake --build . --config Release"
                ))
            }
            else {
                // Linux / macOS 直接 build
                execOpt.exec(nativeBuildDir, "sh", "-c", buildArgs(
                    "cmake .. $cmakeArgsText",
                    "make -j 4"
                ))
            }
            println("Build \"$libName\" for desktop targets complete.")
        }
    }
}