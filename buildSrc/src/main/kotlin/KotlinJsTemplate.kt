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

abstract class KotlinJsTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    open fun KotlinJsSourceSetsScope.source() { }
    open fun KotlinJsTargetDsl.js() { }
    open fun KotlinWebpackConfig.webpack() { }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            js {
                outputModuleName.set(uniqueModuleName)
                browser {
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
                    target.set("es2015")
                    useLanguageFeature(
                        "-Xes-long-as-bigint"
                    )
                }

                js()
            }

            // SourceSet
            KotlinJsSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        afterEvaluate {
            actions()
        }
    }
}