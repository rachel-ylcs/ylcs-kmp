package love.yinlin.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

class KotlinJsSourceSetsScope(
    p: Project,
    private val extension: KotlinMultiplatformExtension,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val commonMain: KotlinSourceSet by lazy { with(extension) { set.commonMain.get() } }
    val commonTest: KotlinSourceSet by lazy { with(extension) { set.commonTest.get() } }
    val jsMain: KotlinSourceSet by lazy { with(extension) { set.jsMain.get() } }
}

open class KotlinJsTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    private var sourceBlock: KotlinJsSourceSetsScope.() -> Unit = {}
    fun sources(block: KotlinJsSourceSetsScope.() -> Unit) {
        val previous = sourceBlock
        sourceBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinJsSourceSetsScope.source() { sourceBlock(this) }
    private var jsBlock: KotlinJsTargetDsl.() -> Unit = {}
    fun js(block: KotlinJsTargetDsl.() -> Unit) {
        val previous = jsBlock
        jsBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinJsTargetDsl.js() { jsBlock(this) }
    private var webpackBlock: KotlinWebpackConfig.() -> Unit = {}
    fun webpack(block: KotlinWebpackConfig.() -> Unit) {
        val previous = webpackBlock
        webpackBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinWebpackConfig.webpack() { webpackBlock(this) }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            js {
                outputModuleName.set(uniqueModuleName)
                browser {
                    testTask {
                        useKarma {
                            useChromeHeadless()
                        }
                    }

                    commonWebpackConfig {
                        cssSupport {
                            enabled.set(true)
                        }

                        webpack()
                    }
                }
                binaries.executable()
                generateTypeScriptDefinitions()
                compilerOptions {
                    target.set(buildDefaults.jsTarget)
                    useLanguageFeature()
                }

                js()
            }

            // SourceSet
            KotlinJsSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        configureActions()
    }
}
