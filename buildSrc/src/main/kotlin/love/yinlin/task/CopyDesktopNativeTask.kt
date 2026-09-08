package love.yinlin.task

import C
import defaultNativeModuleName
import enumSubModules
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import packageResourcesDir
import kotlin.io.resolve

abstract class CopyDesktopNativeTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun copyNativeLibs() {
        // 确定 native 库目录和 appResources 目录
        val libSourceDir = project.C.root.artifacts.desktopNative.asFile
        val targetResourcesDir = project.packageResourcesDir.dir(project.C.resourceTag).asFile
        targetResourcesDir.mkdirs()

        // 复制 native 库
        val libOutputList = mutableListOf<String>()
        for (subModuleSelector in project.enumSubModules) {
            val moduleName = System.mapLibraryName(defaultNativeModuleName(subModuleSelector))
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