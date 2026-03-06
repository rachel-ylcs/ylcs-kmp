import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val androidNativeTarget: Boolean = true
    override val windowsTarget: Boolean = true
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.context
            )
        }

        val clientMain by create(commonMain)

        nativeMain.configure(clientMain)

        val jvmMain by create(clientMain)

        appleMain.configure(nativeMain)

        androidMain.configure(jvmMain)

        iosMain.configure(appleMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        androidNativeMain.configure(nativeMain)

        windowsMain.configure(nativeMain)

        windowsTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }

        linuxMain.configure(nativeMain)

        macosMain.configure(appleMain)
    }

    override fun KotlinNativeTarget.windows() {
        compilations.getByName("main") {
            val win32 by cinterops.creating
        }
    }
})