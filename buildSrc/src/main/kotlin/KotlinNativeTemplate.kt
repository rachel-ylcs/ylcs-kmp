import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KotlinNativeSourceSetsScope(
    private val p: Project,
    private val extension: KotlinMultiplatformExtension,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(set) {
    val commonMain: KotlinSourceSet by lazy { with(extension) { set.commonMain.get() } }
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

    open val windowsTarget: Boolean = false
    open val linuxTarget: Boolean = false
    open val macosTarget: Boolean = false
    open fun KotlinNativeTarget.native() { }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
            }

            if (windowsTarget) mingwX64("windows") { native() }
            if (linuxTarget) linuxX64("linux") { native() }
            if (macosTarget) macosArm64("macos") { native() }

            // SourceSet
            KotlinNativeSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        afterEvaluate {
            actions()
        }
    }
}