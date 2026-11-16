package love.yinlin.gradle

import love.yinlin.gradle.task.CollectComposeStabilityTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.io.File

private const val ComposeStabilityTaskName = "collectComposeStabilityConfigurations"
private const val ComposeCompilerPluginId = "org.jetbrains.kotlin.plugin.compose"
private const val KotlinMultiplatformPluginId = "org.jetbrains.kotlin.multiplatform"

/**
 * 为 Compose Compiler 模块注册稳定性配置收集
 */
internal fun Project.configureComposeStability() {
    val ownStabilityFiles = files({
        layout.projectDirectory.dir("config").asFile
    }).asFileTree.matching {
        include("*.conf")
    }

    pluginManager.withPlugin(ComposeCompilerPluginId) {
        val composeCompiler = extensions.getByType<ComposeCompilerGradlePluginExtension>()
        val collectTask = tasks.register(ComposeStabilityTaskName, CollectComposeStabilityTask::class.java) {
            stabilityFiles.from(ownStabilityFiles)
        }

        composeCompiler.stabilityConfigurationFiles.add(collectTask.flatMap { it.outputFile.locationOnly })

        tasks.withType<AbstractKotlinCompileTool<*>>().configureEach {
            dependsOn(collectTask)
        }

        if (pluginManager.hasPlugin(KotlinMultiplatformPluginId)) {
            val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
            kotlin.targets.configureEach {
                compilations.configureEach {
                    val compileConfigurationName = compileDependencyConfigurationName
                    this@configureComposeStability.configurations.matching {
                        it.name == compileConfigurationName
                    }.configureEach {
                        if (isCanBeResolved) {
                            val dependencyFiles = this@configureComposeStability.composeStabilityFiles(this)
                            collectTask.configure {
                                stabilityFiles.from(dependencyFiles)
                            }
                        }
                    }
                }
            }
        } else {
            configurations.configureEach {
                if (isCanBeResolved && name.endsWith("compileClasspath", ignoreCase = true)) {
                    val dependencyFiles = this@configureComposeStability.composeStabilityFiles(this)
                    collectTask.configure {
                        stabilityFiles.from(dependencyFiles)
                    }
                }
            }
        }
    }
}

private fun Project.composeStabilityFiles(configuration: Configuration): Provider<Set<File>> {
    val configDirectories = rootProject.allprojects.associate {
        it.path to it.layout.projectDirectory.dir("config").asFile
    }
    val currentProjectPath = path

    return providers.provider {
        configuration.incoming.resolutionResult.allComponents
            .asSequence()
            .mapNotNull { it.id as? ProjectComponentIdentifier }
            .filter { it.projectPath != currentProjectPath && it.projectPath in configDirectories }
            .flatMap { component ->
                val directory = configDirectories[component.projectPath]
                directory?.listFiles { file ->
                    file.isFile && file.extension.equals("conf", ignoreCase = true)
                }?.asSequence() ?: emptySequence()
            }
            .toSet()
    }
}
