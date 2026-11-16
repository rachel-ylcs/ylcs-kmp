package love.yinlin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val ComposeStabilityUsage = "rachel-compose-stability"
private const val ComposeStabilityElements = "rachelComposeStabilityElements"
private const val ComposeCompilerPluginId = "org.jetbrains.kotlin.plugin.compose"
private const val KotlinMultiplatformPluginId = "org.jetbrains.kotlin.multiplatform"

/**
 * 将模块自己的 Compose 稳定性配置发布为项目工件。
 *
 * 普通模块只发布 config 目录中的 .conf 文件，不需要应用 Compose Compiler；
 * 应用 Compose Compiler 的模块则从自身及传递项目依赖中收集这些配置。
 */
internal fun Project.configureComposeStability() {
    val stabilityFiles = files({
        layout.projectDirectory.dir("config").asFile
    }).asFileTree.matching {
        include("*.conf")
    }

    configurations.create(ComposeStabilityElements) {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(ComposeStabilityUsage))
        outgoing.artifacts(stabilityFiles.elements)
    }

    pluginManager.withPlugin(ComposeCompilerPluginId) {
        val composeCompiler = extensions.getByType<ComposeCompilerGradlePluginExtension>()
        composeCompiler.stabilityConfigurationFiles.from(stabilityFiles)

        // Android / Kotlin JVM 等非 KMP 模块的编译 classpath。
        configurations.configureEach {
            if (isCanBeResolved && name.endsWith("compileClasspath", ignoreCase = true)) {
                composeCompiler.stabilityConfigurationFiles.from(
                    this@configureComposeStability.composeStabilityFiles(this)
                )
            }
        }

        // KMP 各 target 的编译依赖配置不都以 CompileClasspath 命名，
        // 因此通过 KotlinCompilation 提供的配置名逐一收集。
        pluginManager.withPlugin(KotlinMultiplatformPluginId) {
            extensions.findByType<KotlinMultiplatformExtension>()?.targets?.configureEach {
                compilations.configureEach {
                    val compileConfigurationName = compileDependencyConfigurationName
                    this@configureComposeStability.configurations.matching {
                        it.name == compileConfigurationName
                    }.configureEach {
                        if (isCanBeResolved) {
                            composeCompiler.stabilityConfigurationFiles.from(
                                this@configureComposeStability.composeStabilityFiles(this)
                            )
                        }
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
