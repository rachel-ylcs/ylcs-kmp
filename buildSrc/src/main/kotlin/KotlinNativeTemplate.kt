import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KotlinNativeSourceSetsScope(
    p: Project,
    private val extension: KotlinMultiplatformExtension,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val commonMain: KotlinSourceSet by lazy { with(extension) { set.commonMain.get() } }
    val androidNativeMain: KotlinSourceSet by lazy { set.getByName("androidNativeMain") }
    val androidNativeTest: KotlinSourceSet by lazy { set.getByName("androidNativeTest") }
    val windowsMain: KotlinSourceSet by lazy { set.getByName("windowsMain") }
    val windowsTest: KotlinSourceSet by lazy { set.getByName("windowsTest") }
    val linuxMain: KotlinSourceSet by lazy { set.getByName("linuxMain") }
    val linuxTest: KotlinSourceSet by lazy { set.getByName("linuxTest") }
    val macosMain: KotlinSourceSet by lazy { set.getByName("macosMain") }
    val macosTest: KotlinSourceSet by lazy { set.getByName("macosTest") }
}

abstract class KotlinNativeTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    // SourceSets
    abstract val libName: String
    open fun KotlinNativeSourceSetsScope.source() { }

    open val androidNativeTarget: Boolean = false
    open val windowsTarget: Boolean = false
    open val linuxTarget: Boolean = false
    open val macosTarget: Boolean = false
    open fun KotlinNativeTarget.native() { }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            buildList {
                if (androidNativeTarget) add(androidNativeArm64("androidNative"))
                if (windowsTarget) add(mingwX64("windows"))
                if (linuxTarget) add(linuxX64("linux"))
                if (macosTarget) add(macosArm64("macos"))
            }.forEach { target ->
                target.compilerOptions {
                    useLanguageFeature()
                }
                target.native()
            }

            // SourceSet
            KotlinNativeSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        afterEvaluate {
            actions()
        }
    }
}