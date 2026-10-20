package love.yinlin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.Directory
import org.gradle.kotlin.dsl.getByType

val Project.buildDefaults: BuildDefaults get() = extensions.getByType<BuildDefaults>()

@Suppress("UnstableApiUsage")
internal fun Project.findProjectDirectory(dependency: ProjectDependency): Directory? = findProject(dependency.path)?.isolated?.projectDirectory

// KMP 项目资源目录
val Project.packageResourcesDir: Directory get() = layout.buildDirectory.get().dir("packageResources")

// 生成代码源目录
val Project.generateSourceDir: Directory get() = layout.buildDirectory.get().dir("generated").dir("kotlin")

// Android Proguard 目录
val Project.androidProguardKMPDir: Directory get() = layout.projectDirectory.dir("src").dir("androidMain").dir("resources").dir("META-INF").dir("proguard")
val Project.androidProguardAndroidDir: Directory get() = layout.projectDirectory.dir("src").dir("main").dir("resources").dir("META-INF").dir("proguard")

// iOS Local SPM 目录
val Project.iosLocalSPMDir: Directory get() = layout.projectDirectory.dir("src").dir("iosMain").dir("spm")

// Desktop 二进制输出目录
val Project.desktopBinaryDir: Directory get() = layout.buildDirectory.get().dir("compose").dir("binaries").dir("main-release").dir("app")

// Desktop Proguard 目录
val Project.desktopProguardKMPDir: Directory get() = layout.projectDirectory.dir("src").dir("desktopMain").dir("resources").dir("META-INF").dir("proguard")
val Project.desktopProguardJVMDir: Directory get() = layout.projectDirectory.dir("src").dir("main").dir("resources").dir("META-INF").dir("proguard")

// Desktop Native 编译目录
val Project.desktopNativeBuildDir: Directory get() = layout.buildDirectory.get().dir("desktopNative")

// Desktop Native 源代码
val Project.desktopNativeKMPSourceDir: Directory get() = layout.projectDirectory.dir("src").dir("desktopMain").dir("cpp")
val Project.desktopNativeJVMSourceDir: Directory get() = layout.projectDirectory.dir("src").dir("main").dir("cpp")

// Web 二进制输出目录
val Project.jsBinaryDir: Directory get() = layout.buildDirectory.get().dir("dist").dir("js").dir("productionExecutable")
val Project.wasmBinaryDir: Directory get() = layout.buildDirectory.get().dir("dist").dir("wasmJs").dir("productionExecutable")

// Native 二进制输出目录
fun Project.nativeBinaryDir(platform: String, filename: String) = layout.buildDirectory.get().dir("bin").dir(platform).dir("releaseExecutable").file(filename)

// KMP 资源
val Project.hasKMPResources: Boolean get() = layout.projectDirectory.dir("src").asFile.listFiles {
    it.isDirectory && (it.resolve("composeResources").isDirectory || it.resolve("res").isDirectory)
}?.isNotEmpty() ?: false

fun defaultNativeModuleName(path: String): String = path.removePrefix(":").replace(':', '.')
