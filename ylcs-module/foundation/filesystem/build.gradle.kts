import love.yinlin.gradle.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        webTestHeadless = false

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.context
                )
            }

            commonTest {
                lib(
                    libs.test,
                    libs.kotlinx.coroutines.test
                )
            }

            val jvmMain = createJvm(commonMain)

            nativeMain(commonMain)

            appleMain(nativeMain)

            androidMain(jvmMain)

            iosMain(appleMain)

            iosMainList(iosMain)

            desktopMain(jvmMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(appleMain)
        }

        windows {
            compilations.getByName("main") {
                cinterops.create("win32")
            }
        }
    }
}
