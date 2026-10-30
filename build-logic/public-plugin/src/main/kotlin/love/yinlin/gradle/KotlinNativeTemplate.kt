package love.yinlin.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
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

    var nativeDesktopTarget: Boolean = false
    private var windowsTargetOverride: Boolean? = null
    private var linuxTargetOverride: Boolean? = null
    private var macosTargetOverride: Boolean? = null
    var windowsTarget: Boolean
        get() = windowsTargetOverride ?: nativeDesktopTarget
        set(value) { windowsTargetOverride = value }
    var linuxTarget: Boolean
        get() = linuxTargetOverride ?: nativeDesktopTarget
        set(value) { linuxTargetOverride = value }
    var macosTarget: Boolean
        get() = macosTargetOverride ?: nativeDesktopTarget
        set(value) { macosTargetOverride = value }
    private var windowsBlock: KotlinNativeTarget.() -> Unit = {}
    fun windows(block: KotlinNativeTarget.() -> Unit) {
        val previous = windowsBlock
        windowsBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.windows() { windowsBlock(this) }
    private var linuxBlock: KotlinNativeTarget.() -> Unit = {}
    fun linux(block: KotlinNativeTarget.() -> Unit) {
        val previous = linuxBlock
        linuxBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.linux() { linuxBlock(this) }
    private var macosBlock: KotlinNativeTarget.() -> Unit = {}
    fun macos(block: KotlinNativeTarget.() -> Unit) {
        val previous = macosBlock
        macosBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.macos() { macosBlock(this) }
    private var nativeBlock: KotlinNativeTarget.() -> Unit = {}
    fun native(block: KotlinNativeTarget.() -> Unit) {
        val previous = nativeBlock
        nativeBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.native() { nativeBlock(this) }

    open fun KotlinNativeTarget.baseNative() { native() }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
            }

            val nativeTargets = buildList {
                if (windowsTarget) add(mingwX64("windows") { windows() })
                if (linuxTarget) add(linuxX64("linux") { linux() })
                if (macosTarget) add(macosArm64("macos") { macos() })
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

        if (hasKMPResources) {
            extensions.findByType<ComposeExtension>()?.extensions?.findByType<ResourcesExtension>()?.apply {
                publicResClass = true
                packageOfResClass = "$uniqueSafeModuleName.resources"
            }
        }

        configureActions()
        registerNativeCpp(extension)
    }
}
