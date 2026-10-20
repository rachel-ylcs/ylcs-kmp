import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.compose.ui,
                    projects.ylcsModule.compose.platformView,
                )
            }

            commonTest {
                lib(libs.test)
            }

            androidMain(commonMain) {
                lib(
                    libs.scan.android,
                    libs.scan.camera.android
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
