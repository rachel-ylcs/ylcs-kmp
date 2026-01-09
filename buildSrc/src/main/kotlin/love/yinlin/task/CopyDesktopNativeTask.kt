package love.yinlin.task

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.io.resolve

abstract class CopyDesktopNativeTask : DefaultTask() {
    @get:Input
    abstract val moduleList: ListProperty<String>

    @get:Input
    abstract val libDir: Property<File>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copyNative() {
        val packageResourcesDir = outputDir.get().asFile
        val libSourceDir = libDir.get()
        packageResourcesDir.mkdirs()

        val libOutputList = mutableListOf<String>()
        for (moduleName in moduleList.get()) {
            val libName = System.mapLibraryName(moduleName)
            val libFile = libSourceDir.resolve(libName)
            val outputFile = packageResourcesDir.resolve(libName)
            if (libFile.exists()) {
                libOutputList += libName
                libFile.copyTo(outputFile, true)
            }
        }
        println("Copy Desktop Native Library: $libOutputList")
    }

    companion object {
        val Project.moduleDependencies: List<String> get() {
            val projectDeps = mutableSetOf<String>()
            configurations.forEach { config ->
                if (config.isCanBeResolved) {
                    runCatching {
                        config.incoming.resolutionResult.allComponents.forEach {
                            if (it.id is ProjectComponentIdentifier) projectDeps += it.id.displayName
                        }
                    }
                }
            }
            return projectDeps.toList()
        }
    }
}