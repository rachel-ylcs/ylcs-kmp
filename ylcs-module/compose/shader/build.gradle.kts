import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.compose.core
                )
            }

            commonTest {
                lib(libs.test)
            }

            skikoMain(commonMain)

            iosMain(skikoMain)

            iosMainList(iosMain)

            desktopMain(skikoMain)
            desktopTest {
                dependencies {
                    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-all:${libs.versions.skiko.get()}")
                }
            }

            nativeMain(skikoMain)

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)

            webMain(skikoMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
