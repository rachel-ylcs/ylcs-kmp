import love.yinlin.project.Constants
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.Directory
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import kotlin.collections.forEach

enum class BuildEnvironment { Dev, Prod }

enum class BuildPlatform {
    Windows, Linux, Mac;

    override fun toString(): String = when (this) {
        Windows -> "win"
        Linux -> "linux"
        Mac -> "mac"
    }
}

enum class BuildArchitecture {
    X86_64, ARM, AARCH64;

    override fun toString(): String = when (this) {
        X86_64 -> "x86_64"
        ARM -> "arm"
        AARCH64 -> "aarch64"
    }
}

val projectMap = mutableMapOf<Project, Constants>()
// 当前项目常量
val Project.C: Constants get() = projectMap.getOrPut(this) { Constants(this) }

// 当前任务名
val Project.currentTaskName: String get() = gradle.startParameter.taskNames.firstOrNull() ?: "sync"

// 查找项目
fun Project.findProject(dependency: DelegatingProjectDependency): Project? = rootProject.findProject(dependency.path)

// KMP 项目资源目录
val Project.packageResourcesDir: Directory get() = layout.buildDirectory.get().dir("packageResources")

// 生成代码源目录
val Project.generateSourceDir: Directory get() = layout.buildDirectory.get().dir("generated").dir("kotlin")

// Android Proguard 目录
val Project.androidProguardKMPDir: Directory get() = layout.projectDirectory.dir("src").dir("androidMain").dir("resources").dir("META-INF").dir("proguard")
val Project.androidProguardAndroidDir: Directory get() = layout.projectDirectory.dir("src").dir("main").dir("resources").dir("META-INF").dir("proguard")

// Desktop Proguard 目录
val Project.desktopProguardKMPDir: Directory get() = layout.projectDirectory.dir("src").dir("desktopMain").dir("resources").dir("META-INF").dir("proguard")
val Project.desktopProguardJVMDir: Directory get() = layout.projectDirectory.dir("src").dir("main").dir("resources").dir("META-INF").dir("proguard")

// Desktop Native 编译目录
val Project.desktopNativeBuildDir: Directory get() = layout.buildDirectory.get().dir("desktopNative")

// Desktop Native 源代码
val Project.desktopNativeKMPSourceDir: Directory get() = layout.projectDirectory.dir("src").dir("desktopMain").dir("cpp")
val Project.desktopNativeJVMSourceDir: Directory get() = layout.projectDirectory.dir("src").dir("main").dir("cpp")

// KMP 资源
val Project.hasKMPResources: Boolean get() = layout.projectDirectory.dir("src").asFile.listFiles {
    it.isDirectory && (it.resolve("composeResources").isDirectory || it.resolve("res").isDirectory)
}?.isNotEmpty() ?: false

// 获取所有子模块(包含自身)的Selector
// 形如 :ylcs-module:compose:app
val Project.enumSubModules: List<String> get() {
    val moduleList = mutableSetOf<String>()
    configurations.forEach { config ->
        if (config.isCanBeResolved) {
            runCatching {
                for (result in config.incoming.resolutionResult.allComponents) {
                    val id = result.id as? ProjectComponentIdentifier ?: continue
                    moduleList += id.projectPath
                }
            }
        }
    }
    return moduleList.toList()
}

// 获取模块对应的动态链接库名称
fun defaultNativeModuleName(path: String): String = path.removePrefix(":").replace(':', '.')