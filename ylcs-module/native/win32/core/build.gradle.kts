import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinNativeLibTemplate() {
    override val windowsTarget: Boolean = true

    override fun KotlinNativeSourceSetsScope.source() {
        windowsMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.core,
            )
        }

        windowsTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }
    }

    override fun KotlinNativeTarget.native() {
        compilations.getByName("main") {
            val win32 by cinterops.creating
        }
    }
})