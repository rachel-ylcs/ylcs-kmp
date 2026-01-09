package love.yinlin.task

import C
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.tasks.TaskAction
import packageResourcesDir
import kotlin.io.resolve

abstract class CopyDesktopNativeTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun copyNativeLibs() {
        // 解析项目依赖的 native 库
        val moduleList = mutableSetOf<String>()
        project.configurations.forEach { config ->
            if (config.isCanBeResolved) {
                runCatching {
                    config.incoming.resolutionResult.allComponents.forEach {
                        if (it.id is ProjectComponentIdentifier) {
                            val moduleName = it.id.displayName.substringAfterLast(':').replace('-', '_')
                            moduleList += System.mapLibraryName(moduleName)
                        }
                    }
                }
            }
        }

        // 确定 native 库目录和 appResources 目录
        val libSourceDir = project.C.root.artifacts.desktopNative.asFile
        val targetResourcesDir = project.packageResourcesDir.dir(project.C.resourceTag).asFile
        targetResourcesDir.mkdirs()

        // 复制 native 库
        val libOutputList = mutableListOf<String>()
        for (moduleName in moduleList) {
            val libFile = libSourceDir.resolve(moduleName)
            val outputFile = targetResourcesDir.resolve(moduleName)
            if (libFile.exists()) {
                libOutputList += moduleName
                libFile.copyTo(outputFile, true)
            }
        }

        println("[CopyDesktopNative] copy desktop native library: $libOutputList")
    }
}