package love.yinlin.gradle

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
    val nativeMain: KotlinSourceSet by lazy { with(extension) { set.nativeMain.get() } }
    val nativeTest: KotlinSourceSet by lazy { with(extension) { set.nativeTest.get() } }
    val windowsMain: KotlinSourceSet by lazy { set.getByName("windowsMain") }
    val windowsTest: KotlinSourceSet by lazy { set.getByName("windowsTest") }
    val linuxMain: KotlinSourceSet by lazy { set.getByName("linuxMain") }
    val linuxTest: KotlinSourceSet by lazy { set.getByName("linuxTest") }
    val macosMain: KotlinSourceSet by lazy { set.getByName("macosMain") }
    val macosTest: KotlinSourceSet by lazy { set.getByName("macosTest") }
}

open class KotlinNativeTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    // SourceSets
    private var sourceBlock: KotlinNativeSourceSetsScope.() -> Unit = {}
    fun sources(block: KotlinNativeSourceSetsScope.() -> Unit) {
        val previous = sourceBlock
        sourceBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeSourceSetsScope.source() { sourceBlock(this) }

    var windowsTarget: Boolean = false
    var linuxTarget: Boolean = false
    var macosTarget: Boolean = false
    open fun KotlinNativeTarget.baseNative() { }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
            }

            val nativeTargets = buildList {
                if (windowsTarget) add(mingwX64("windows"))
                if (linuxTarget) add(linuxX64("linux"))
                if (macosTarget) add(macosArm64("macos"))
            }
            val nativeMain = sourceSets.maybeCreate("nativeMain").apply { dependsOn(sourceSets.commonMain.get()) }
            val nativeTest = sourceSets.maybeCreate("nativeTest").apply { dependsOn(sourceSets.commonTest.get()) }
            nativeTargets.forEach { target ->
                target.compilations.getByName("main").defaultSourceSet.dependsOn(nativeMain)
                target.compilations.getByName("test").defaultSourceSet.dependsOn(nativeTest)
                target.baseNative()
            }

            // SourceSet
            KotlinNativeSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        configureActions()
    }
}
