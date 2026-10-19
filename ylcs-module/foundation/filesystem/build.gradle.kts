import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val windowsTarget: Boolean = true
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

    override val webTestHeadless: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.context
            )
        }

        commonTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test
            )
        }

        val jvmMain = createJvm(commonMain)

        nativeMain.configure(commonMain)

        appleMain.configure(nativeMain)

        androidMain.configure(jvmMain)

        iosMain.configure(appleMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain)

        webMain.configure(commonMain)

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)

        windowsMain.configure(nativeMain)

        linuxMain.configure(nativeMain)

        macosMain.configure(appleMain)
    }

    override fun KotlinNativeTarget.windows() {
        compilations.getByName("main") {
            cinterops.create("win32")
        }
    }
})