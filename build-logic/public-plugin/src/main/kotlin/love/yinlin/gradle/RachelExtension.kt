package love.yinlin.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

/**
 * Declares one Kotlin module template. Options are collected before targets and sources are created.
 * Apply optional Compose, Android, serialization and publishing plugins in the plugins block.
 */
class RachelExtension(private val project: Project) {
    private var configured = false

    fun defaults(block: BuildDefaults.() -> Unit) {
        check(!configured) { "Configure defaults before declaring the module template." }
        project.buildDefaults.block()
    }

    fun multiplatform(block: KotlinMultiplatformTemplate.() -> Unit) =
        configure(KotlinMultiplatformTemplate(), "org.jetbrains.kotlin.multiplatform", block)

    fun jvm(block: KotlinJvmTemplate.() -> Unit) =
        configure(KotlinJvmTemplate(), "org.jetbrains.kotlin.jvm", block)

    fun js(block: KotlinJsTemplate.() -> Unit) =
        configure(KotlinJsTemplate(), "org.jetbrains.kotlin.multiplatform", block)

    fun androidApplication(block: KotlinAndroidTemplate.() -> Unit) =
        configure(KotlinAndroidTemplate(), "com.android.application", block)

    fun androidLibrary(block: KotlinAndroidNDKTemplate.() -> Unit) =
        configure(KotlinAndroidNDKTemplate(), "com.android.library", block)

    fun native(block: KotlinNativeTemplate.() -> Unit) =
        configure(KotlinNativeTemplate(), "org.jetbrains.kotlin.multiplatform", block)

    fun nativeLibrary(block: KotlinNativeLibTemplate.() -> Unit) =
        configure(KotlinNativeLibTemplate(), "org.jetbrains.kotlin.multiplatform", block)

    fun nativeExecutable(block: KotlinNativeExecutableTemplate.() -> Unit) =
        configure(KotlinNativeExecutableTemplate(), "org.jetbrains.kotlin.multiplatform", block)

    private inline fun <reified T : KotlinBaseExtension, S : KotlinTemplate<T>> configure(
        template: S,
        plugin: String,
        block: S.() -> Unit,
    ) {
        check(!configured) { "Only one Rachel module template can be declared in ${project.path}." }
        configured = true
        project.pluginManager.apply(plugin)
        val defaults = project.buildDefaults
        val namespace = defaults.namespace.ifEmpty {
            project.group.toString().takeUnless { it.isEmpty() || it == "unspecified" } ?: "project"
        }
        template.owner = project
        template.internalBackendInfo = BackendInfo(
            namespace,
            project.path,
            project.layout.projectDirectory.asFile.absolutePath,
            defaults.stripFirstPathSegment,
            project.name,
        )
        template.block()
        project.configureTemplate(template)
    }
}
