import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinNativeTemplate() {
    override val windowsTarget: Boolean = true

    override fun KotlinNativeTarget.native() {
        binaries {
            sharedLib {
                baseName = "platform_lib"
            }
        }
    }

    override fun KotlinNativeSourceSetsScope.source() {
        windowsMain.configure {
            lib(
                ExportLib,
                projects.ylcsBase.core,
            )
        }

        windowsTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }
    }
})