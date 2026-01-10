import love.yinlin.project.Constants
import org.gradle.api.Project
import org.gradle.api.file.Directory

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
val Project.currentTaskName: String get() = this.gradle.startParameter.taskNames.firstOrNull() ?: "sync"

// KMP 项目资源目录
val Project.packageResourcesDir: Directory get() = layout.buildDirectory.get().dir("packageResources")

// 生成代码源目录
val Project.generateSourceDir: Directory get() = layout.buildDirectory.get().dir("generated").dir("kotlin")