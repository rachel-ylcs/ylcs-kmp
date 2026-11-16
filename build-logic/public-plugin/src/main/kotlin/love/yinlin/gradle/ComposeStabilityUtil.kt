package love.yinlin.gradle

import love.yinlin.gradle.task.CollectComposeStabilityTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val ComposeStabilityUsage = "rachel-compose-stability"
private const val ComposeStabilityElements = "rachelComposeStabilityElements"
private const val ComposeStabilityTaskName = "collectComposeStabilityConfigurations"
private const val ComposeCompilerPluginId = "org.jetbrains.kotlin.plugin.compose"
private const val KotlinMultiplatformPluginId = "org.jetbrains.kotlin.multiplatform"

/**
 * 将模块自己的 Compose 稳定性配置发布为项目工件。
 *
 * 普通模块只发布 config 目录中的 .conf 文件，不需要应用 Compose Compiler；
 * 应用 Compose Compiler 的模块则通过独立任务收集自身及传递项目依赖中的配置。
 */
internal fun Project.configureComposeStability() {
    val ownStabilityFiles = files({
        layout.projectDirectory.dir("config").asFile
    }).asFileTree.matching {
        include("*.conf")
    }

    configurations.create(ComposeStabilityElements) {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(ComposeStabilityUsage))
        outgoing.artifacts(ownStabilityFiles.elements)
    }

    pluginManager.withPlugin(ComposeCompilerPluginId) {
        val composeCompiler = extensions.getByType<ComposeCompilerGradlePluginExtension>()
        val collectTask = tasks.register(ComposeStabilityTaskName, CollectComposeStabilityTask::class.java) {
            stabilityFiles.from(ownStabilityFiles)
        }

        // The compiler receives only the task output provider. Project artifact views are resolved
        // by the task at execution time, after every project has completed configuration.
        composeCompiler.stabilityConfigurationFiles.add(collectTask.flatMap { it.outputFile })

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
            // Android / Kotlin JVM modules expose their compile dependencies through compileClasspath.
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

@Suppress("UnstableApiUsage")
private fun Project.composeStabilityFiles(configuration: Configuration): FileCollection =
    configuration.incoming.artifactView {
        isLenient = true
        componentFilter { it is ProjectComponentIdentifier }
        withVariantReselection()
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(ComposeStabilityUsage))
    }.files
