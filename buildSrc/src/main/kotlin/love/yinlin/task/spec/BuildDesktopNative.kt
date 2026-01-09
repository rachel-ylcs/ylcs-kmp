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

fun KotlinJvmTemplate.registerCopyDesktopNative(project: Project) = project.copyDesktopNative("jar")
fun KotlinMultiplatformTemplate.registerCopyDesktopNative(project: Project) = project.copyDesktopNative("desktopJar")

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
            packageResourcesDir.deleteRecursively()
        }
    }
}