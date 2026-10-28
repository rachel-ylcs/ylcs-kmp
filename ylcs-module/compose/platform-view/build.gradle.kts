import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
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
                    projects.ylcsModule.compose.core,
                )
            }

            commonTest {
                lib(libs.test, libs.kotlinx.coroutines.test)
            }

            androidMain(commonMain) {
                lib(libs.androidx.activity.compose)
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }
    }
}
