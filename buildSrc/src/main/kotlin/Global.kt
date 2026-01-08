import love.yinlin.project.Constants
import org.gradle.api.Project

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
// 获取当前项目常量
val Project.C: Constants get() = projectMap.getOrPut(this) { Constants(this) }

// 获取当前任务名
val Project.currentTaskName: String get() = this.gradle.startParameter.taskNames.firstOrNull() ?: "sync"