package love.yinlin.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class KotlinJvmSourceSetsScope(
    p: Project,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val main: KotlinSourceSet by lazy { set.named("main").get() }
    val test: KotlinSourceSet by lazy { set.named("test").get() }
}

open class KotlinJvmTemplate : KotlinTemplate<KotlinJvmExtension>() {
    private var sourceBlock: KotlinJvmSourceSetsScope.() -> Unit = {}
    fun sources(block: KotlinJvmSourceSetsScope.() -> Unit) {
        val previous = sourceBlock
        sourceBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinJvmSourceSetsScope.source() { sourceBlock(this) }

    var jvmName: String = ""
    var jvmMainClass: String? = null
    var jvmArgs: List<String> = emptyList()
    private var applicationBlock: JavaApplication.() -> Unit = {}
    fun application(block: JavaApplication.() -> Unit) {
        val previous = applicationBlock
        applicationBlock = {
            previous()
            block()
        }
    }
    internal fun JavaApplication.application() { applicationBlock(this) }

    final override fun Project.build(extension: KotlinJvmExtension) {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = buildDefaults.jvm.compatibility
            targetCompatibility = buildDefaults.jvm.compatibility
        }

        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(JvmTarget.fromTarget(buildDefaults.jvm.target))
            }

            KotlinJvmSourceSetsScope(this@build, sourceSets).source()

            action()
        }

        extensions.findByType<JavaApplication>()?.apply {
            mainClass.set(jvmMainClass)
            applicationName = jvmName.ifEmpty { uniqueSafeName }
            applicationDefaultJvmArgs = buildList {
                addAll(jvmArgs)
                addAll(buildDefaults.jvm.args)
            }
            application()
        }

        configureActions()
        registerDesktopNative(desktopNativeJVMSourceDir, "jar")
    }
}
