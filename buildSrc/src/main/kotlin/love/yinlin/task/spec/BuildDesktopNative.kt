package love.yinlin.task.spec

import C
import KotlinJvmTemplate
import KotlinMultiplatformTemplate
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import java.io.File

fun KotlinJvmTemplate.checkBuildDesktopNative(project: Project) = project.buildDesktopNative("src/main/cpp", "jar")
fun KotlinMultiplatformTemplate.checkBuildDesktopNative(project: Project) = project.buildDesktopNative("src/desktopMain/cpp", "desktopJar")

fun KotlinJvmTemplate.registerCopyDesktopNative(project: Project) = project.copyDesktopNative("jar")
fun KotlinMultiplatformTemplate.registerCopyDesktopNative(project: Project) = project.copyDesktopNative("desktopJar")

private fun buildArgs(vararg args: String) = args.joinToString(" && ")

private fun ExecOperations.exec(currentDir: File, vararg args: String): Boolean {
    return exec {
        workingDir = currentDir
        standardOutput = System.out
        errorOutput = System.err
        commandLine(*args)
    }.exitValue == 0
}

private fun Project.buildDesktopNative(nativePath: String, jarTaskName: String) {
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

private fun Project.copyDesktopNative(jarTaskName: String) {
    tasks.named(jarTaskName) {
        doLast {
            // 获取所有项目依赖
            val projectDeps = mutableSetOf<String>()
            configurations.forEach { config ->
                if (config.isCanBeResolved) {
                    try {
                        config.incoming.resolutionResult.allComponents.forEach {
                            if (it.id is ProjectComponentIdentifier) {
                                val moduleName = it.id.displayName.substringAfterLast(':').replace('-', '_')
                                projectDeps += moduleName
                            }
                        }
                    }
                    catch (_: Throwable) {}
                }
            }

            // 检查是否存在 native 库
            val libDir = C.root.artifacts.desktopNative.asFile
            val packageResourcesDir = layout.projectDirectory.dir("packageResources").dir(C.resourceTag).asFile
            packageResourcesDir.deleteRecursively()
            packageResourcesDir.mkdirs()
            for (projectDep in projectDeps) {
                val libName = System.mapLibraryName(projectDep)
                val libFile = libDir.resolve(libName)
                val outputFile = packageResourcesDir.resolve(libName)
                if (libFile.exists()) libFile.copyTo(outputFile, true)
            }
            //packageResourcesDir.deleteRecursively()
        }
    }
}